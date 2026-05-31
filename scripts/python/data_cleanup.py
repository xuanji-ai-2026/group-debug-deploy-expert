#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据清洗脚本 - 数据清理、去重与验证
================================================
Author: 孙丽 (EMP-DATA-002)
Version: 1.0.0
"""

import os
import sys
import json
import logging
import argparse
import re
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Set, Tuple, Any, Callable
from dataclasses import dataclass, field
from collections import defaultdict

import pymysql
from pymysql.cursors import DictCursor
import pandas as pd

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(f'data_cleanup_{datetime.now():%Y%m%d}.log')
    ]
)
logger = logging.getLogger(__name__)


@dataclass
class CleanupRule:
    """数据清洗规则"""
    name: str
    condition: str  # SQL条件或Python表达式
    action: str     # delete/update/flag
    description: str
    enabled: bool = True


@dataclass
class CleanupStats:
    """清洗统计"""
    table: str
    rules_applied: int = 0
    records_deleted: int = 0
    records_updated: int = 0
    records_flagged: int = 0
    duplicates_found: int = 0
    duplicates_removed: int = 0
    errors: List[str] = field(default_factory=list)


class DataCleaner:
    """数据清洗器"""
    
    # 预定义清洗规则
    DEFAULT_RULES = [
        CleanupRule(
            name='expired_sessions',
            condition="last_activity < DATE_SUB(NOW(), INTERVAL 30 DAY)",
            action='delete',
            description='删除30天未活动的会话数据'
        ),
        CleanupRule(
            name='invalid_emails',
            condition="email IS NOT NULL AND email NOT LIKE '%@%.%'",
            action='flag',
            description='标记无效邮箱格式'
        ),
        CleanupRule(
            name='empty_records',
            condition="(title IS NULL OR title = '') AND (content IS NULL OR content = '')",
            action='delete',
            description='删除空记录'
        ),
        CleanupRule(
            name='soft_deleted',
            condition="is_deleted = 1 AND updated_at < DATE_SUB(NOW(), INTERVAL 90 DAY)",
            action='delete',
            description='清理90天前的软删除数据'
        )
    ]
    
    def __init__(self, host: str, port: int, user: str, password: str, 
                 database: str, charset: str = 'utf8mb4'):
        self.config = {
            'host': host,
            'port': port,
            'user': user,
            'password': password,
            'database': database,
            'charset': charset,
            'cursorclass': DictCursor
        }
        self.conn = None
        self.stats: List[CleanupStats] = []
    
    def connect(self) -> bool:
        """建立数据库连接"""
        try:
            self.conn = pymysql.connect(**self.config)
            logger.info("数据库连接成功")
            return True
        except Exception as e:
            logger.error(f"数据库连接失败: {e}")
            return False
    
    def close(self):
        """关闭连接"""
        if self.conn:
            self.conn.close()
            logger.info("数据库连接已关闭")
    
    def get_tables(self) -> List[str]:
        """获取所有表"""
        with self.conn.cursor() as cursor:
            cursor.execute("SHOW TABLES")
            return [list(row.values())[0] for row in cursor.fetchall()]
    
    def analyze_table(self, table_name: str) -> Dict:
        """分析表数据质量"""
        stats = {
            'table': table_name,
            'total_records': 0,
            'null_counts': {},
            'duplicate_counts': {},
            'invalid_formats': {}
        }
        
        try:
            with self.conn.cursor() as cursor:
                # 总记录数
                cursor.execute(f"SELECT COUNT(*) as cnt FROM `{table_name}`")
                stats['total_records'] = cursor.fetchone()['cnt']
                
                # 获取列信息
                cursor.execute(f"DESCRIBE `{table_name}`")
                columns = cursor.fetchall()
                
                for col in columns:
                    col_name = col['Field']
                    
                    # NULL值统计
                    cursor.execute(f"""
                        SELECT COUNT(*) as cnt 
                        FROM `{table_name}` 
                        WHERE `{col_name}` IS NULL
                    """)
                    null_count = cursor.fetchone()['cnt']
                    if null_count > 0:
                        stats['null_counts'][col_name] = null_count
                    
                    # 空字符串统计
                    if col['Type'].startswith(('varchar', 'char', 'text')):
                        cursor.execute(f"""
                            SELECT COUNT(*) as cnt 
                            FROM `{table_name}` 
                            WHERE `{col_name}` = ''
                        """)
                        empty_count = cursor.fetchone()['cnt']
                        if empty_count > 0:
                            stats['null_counts'][f"{col_name} (empty)"] = empty_count
                
                logger.info(f"表 {table_name} 分析完成，共 {stats['total_records']} 条记录")
                
        except Exception as e:
            logger.error(f"分析表 {table_name} 失败: {e}")
        
        return stats
    
    def find_duplicates(self, table_name: str, 
                        key_columns: List[str]) -> List[Dict]:
        """
        查找重复记录
        
        Args:
            table_name: 表名
            key_columns: 用于判断重复的列
        """
        key_cols_str = ', '.join([f'`{c}`' for c in key_columns])
        
        sql = f"""
            SELECT {key_cols_str}, COUNT(*) as cnt
            FROM `{table_name}`
            GROUP BY {key_cols_str}
            HAVING cnt > 1
        """
        
        with self.conn.cursor() as cursor:
            cursor.execute(sql)
            return cursor.fetchall()
    
    def remove_duplicates(self, table_name: str, 
                          key_columns: List[str],
                          keep: str = 'first') -> int:
        """
        删除重复记录
        
        Args:
            table_name: 表名
            key_columns: 用于判断重复的列
            keep: 保留策略 - first/last/none
        
        Returns:
            删除的记录数
        """
        try:
            # 获取主键
            with self.conn.cursor() as cursor:
                cursor.execute(f"DESCRIBE `{table_name}`")
                columns = cursor.fetchall()
                pk_column = None
                for col in columns:
                    if col['Key'] == 'PRI':
                        pk_column = col['Field']
                        break
                
                if not pk_column:
                    logger.warning(f"表 {table_name} 没有主键，跳过去重")
                    return 0
            
            # 查找重复组
            duplicates = self.find_duplicates(table_name, key_columns)
            total_removed = 0
            
            for dup in duplicates:
                # 构建条件
                conditions = []
                values = []
                for col in key_columns:
                    conditions.append(f"`{col}` = %s")
                    values.append(dup[col])
                
                where_clause = ' AND '.join(conditions)
                
                with self.conn.cursor() as cursor:
                    # 获取重复记录
                    cursor.execute(f"""
                        SELECT `{pk_column}` 
                        FROM `{table_name}` 
                        WHERE {where_clause}
                        ORDER BY `{pk_column}`
                    """, values)
                    
                    ids = [row[pk_column] for row in cursor.fetchall()]
                    
                    if keep == 'first':
                        ids_to_delete = ids[1:]
                    elif keep == 'last':
                        ids_to_delete = ids[:-1]
                    else:
                        ids_to_delete = ids
                    
                    if ids_to_delete:
                        format_ids = ', '.join(['%s'] * len(ids_to_delete))
                        cursor.execute(f"""
                            DELETE FROM `{table_name}` 
                            WHERE `{pk_column}` IN ({format_ids})
                        """, ids_to_delete)
                        total_removed += cursor.rowcount
            
            self.conn.commit()
            logger.info(f"表 {table_name} 删除 {total_removed} 条重复记录")
            return total_removed
            
        except Exception as e:
            self.conn.rollback()
            logger.error(f"去重失败: {e}")
            return 0
    
    def apply_rule(self, table_name: str, rule: CleanupRule, 
                   dry_run: bool = False) -> int:
        """应用单个清洗规则"""
        if not rule.enabled:
            return 0
        
        try:
            # 先统计影响记录数
            count_sql = f"SELECT COUNT(*) as cnt FROM `{table_name}` WHERE {rule.condition}"
            
            with self.conn.cursor() as cursor:
                cursor.execute(count_sql)
                affected = cursor.fetchone()['cnt']
            
            if affected == 0:
                return 0
            
            logger.info(f"规则 '{rule.name}' 将影响 {affected} 条记录")
            
            if dry_run:
                return affected
            
            # 执行操作
            if rule.action == 'delete':
                sql = f"DELETE FROM `{table_name}` WHERE {rule.condition}"
                with self.conn.cursor() as cursor:
                    cursor.execute(sql)
                    self.conn.commit()
                    return cursor.rowcount
                    
            elif rule.action == 'update':
                # 更新操作需要额外指定SET子句
                logger.warning("UPDATE规则需要额外配置，暂不支持")
                return 0
                
            elif rule.action == 'flag':
                sql = f"UPDATE `{table_name}` SET data_quality_flag = 'INVALID' WHERE {rule.condition}"
                with self.conn.cursor() as cursor:
                    cursor.execute(sql)
                    self.conn.commit()
                    return cursor.rowcount
            
            return 0
            
        except Exception as e:
            logger.error(f"应用规则 '{rule.name}' 失败: {e}")
            return 0
    
    def clean_table(self, table_name: str, 
                    rules: Optional[List[CleanupRule]] = None,
                    dry_run: bool = False) -> CleanupStats:
        """清洗单个表"""
        stats = CleanupStats(table=table_name)
        rules = rules or self.DEFAULT_RULES
        
        logger.info(f"开始清洗表: {table_name}")
        
        for rule in rules:
            try:
                affected = self.apply_rule(table_name, rule, dry_run)
                if affected > 0:
                    stats.rules_applied += 1
                    if rule.action == 'delete':
                        stats.records_deleted += affected
                    elif rule.action == 'flag':
                        stats.records_flagged += affected
                    elif rule.action == 'update':
                        stats.records_updated += affected
            except Exception as e:
                stats.errors.append(f"规则 {rule.name}: {str(e)}")
        
        logger.info(f"表 {table_name} 清洗完成")
        return stats
    
    def validate_data(self, table_name: str, 
                      validations: List[Dict]) -> Dict:
        """
        数据验证
        
        Args:
            validations: 验证规则列表
                [
                    {'column': 'email', 'type': 'email'},
                    {'column': 'phone', 'type': 'phone'},
                    {'column': 'age', 'type': 'range', 'min': 0, 'max': 150}
                ]
        """
        results = {
            'table': table_name,
            'validations': [],
            'total_invalid': 0
        }
        
        for val in validations:
            col = val['column']
            val_type = val['type']
            invalid_count = 0
            invalid_samples = []
            
            try:
                with self.conn.cursor() as cursor:
                    cursor.execute(f"SELECT `{col}` FROM `{table_name}` WHERE `{col}` IS NOT NULL LIMIT 10000")
                    rows = cursor.fetchall()
                    
                    for row in rows:
                        value = row[col]
                        is_valid = True
                        
                        if val_type == 'email':
                            pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
                            is_valid = bool(re.match(pattern, str(value)))
                            
                        elif val_type == 'phone':
                            pattern = r'^1[3-9]\d{9}$'
                            is_valid = bool(re.match(pattern, str(value)))
                            
                        elif val_type == 'range':
                            try:
                                num = float(value)
                                is_valid = val.get('min', float('-inf')) <= num <= val.get('max', float('inf'))
                            except:
                                is_valid = False
                        
                        elif val_type == 'regex':
                            pattern = val.get('pattern', '')
                            is_valid = bool(re.match(pattern, str(value)))
                        
                        if not is_valid:
                            invalid_count += 1
                            if len(invalid_samples) < 5:
                                invalid_samples.append(value)
                
                result = {
                    'column': col,
                    'type': val_type,
                    'invalid_count': invalid_count,
                    'invalid_samples': invalid_samples
                }
                results['validations'].append(result)
                results['total_invalid'] += invalid_count
                
            except Exception as e:
                logger.error(f"验证 {col} 失败: {e}")
        
        return results
    
    def generate_report(self, output_file: Optional[str] = None):
        """生成清洗报告"""
        report = {
            'generated_at': datetime.now().isoformat(),
            'stats': [self._stats_to_dict(s) for s in self.stats]
        }
        
        report_json = json.dumps(report, indent=2, ensure_ascii=False)
        
        if output_file:
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(report_json)
            logger.info(f"报告已保存: {output_file}")
        
        return report
    
    def _stats_to_dict(self, stats: CleanupStats) -> Dict:
        return {
            'table': stats.table,
            'rules_applied': stats.rules_applied,
            'records_deleted': stats.records_deleted,
            'records_updated': stats.records_updated,
            'records_flagged': stats.records_flagged,
            'duplicates_found': stats.duplicates_found,
            'duplicates_removed': stats.duplicates_removed,
            'errors': stats.errors
        }


def main():
    parser = argparse.ArgumentParser(description='数据清洗工具')
    parser.add_argument('--host', default='localhost', help='数据库主机')
    parser.add_argument('--port', type=int, default=3306, help='数据库端口')
    parser.add_argument('--user', required=True, help='用户名')
    parser.add_argument('--password', required=True, help='密码')
    parser.add_argument('--database', required=True, help='数据库名')
    
    parser.add_argument('--action', choices=['analyze', 'clean', 'dedup', 'validate'],
                        default='analyze', help='执行动作')
    parser.add_argument('--tables', nargs='+', help='指定表')
    parser.add_argument('--dry-run', action='store_true', help='试运行')
    
    # 去重参数
    parser.add_argument('--dup-columns', nargs='+', help='去重键列')
    parser.add_argument('--keep', choices=['first', 'last', 'none'], 
                        default='first', help='保留策略')
    
    args = parser.parse_args()
    
    cleaner = DataCleaner(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database
    )
    
    if not cleaner.connect():
        sys.exit(1)
    
    try:
        tables = args.tables or cleaner.get_tables()
        
        if args.action == 'analyze':
            logger.info("执行数据质量分析...")
            for table in tables:
                stats = cleaner.analyze_table(table)
                logger.info(f"分析结果: {json.dumps(stats, indent=2, default=str)}")
                
        elif args.action == 'clean':
            logger.info("执行数据清洗...")
            for table in tables:
                stats = cleaner.clean_table(table, dry_run=args.dry_run)
                cleaner.stats.append(stats)
            cleaner.generate_report('cleanup_report.json')
            
        elif args.action == 'dedup':
            if not args.dup_columns:
                logger.error("去重操作需要指定 --dup-columns")
                sys.exit(1)
            for table in tables:
                removed = cleaner.remove_duplicates(table, args.dup_columns, args.keep)
                logger.info(f"表 {table} 删除 {removed} 条重复记录")
                
        elif args.action == 'validate':
            logger.info("执行数据验证...")
            # 示例验证规则
            validations = [
                {'column': 'email', 'type': 'email'},
                {'column': 'phone', 'type': 'phone'}
            ]
            for table in tables:
                results = cleaner.validate_data(table, validations)
                logger.info(f"验证结果: {json.dumps(results, indent=2, default=str)}")
    
    finally:
        cleaner.close()


if __name__ == '__main__':
    main()