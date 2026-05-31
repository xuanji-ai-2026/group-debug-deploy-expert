#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据迁移脚本 - 旧系统数据导入与转换
================================================
Author: 孙丽 (EMP-DATA-002)
Version: 1.0.0
"""

import os
import sys
import json
import logging
import argparse
from datetime import datetime
from typing import Dict, List, Optional, Any, Callable
from dataclasses import dataclass
from contextlib import contextmanager

import pymysql
import pymongo
from pymysql.cursors import DictCursor

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(f'data_migration_{datetime.now():%Y%m%d}.log')
    ]
)
logger = logging.getLogger(__name__)


@dataclass
class DatabaseConfig:
    """数据库配置类"""
    host: str
    port: int
    user: str
    password: str
    database: str
    charset: str = 'utf8mb4'
    
    def to_mysql_config(self) -> Dict:
        return {
            'host': self.host,
            'port': self.port,
            'user': self.user,
            'password': self.password,
            'database': self.database,
            'charset': self.charset,
            'cursorclass': DictCursor
        }


class DataMigrator:
    """数据迁移器主类"""
    
    def __init__(self, source_config: DatabaseConfig, target_config: DatabaseConfig):
        self.source_config = source_config
        self.target_config = target_config
        self.source_conn = None
        self.target_conn = None
        self.migration_stats = {
            'tables_processed': 0,
            'records_migrated': 0,
            'records_failed': 0,
            'errors': []
        }
    
    def connect(self) -> bool:
        """建立数据库连接"""
        try:
            logger.info("正在连接源数据库...")
            self.source_conn = pymysql.connect(**self.source_config.to_mysql_config())
            logger.info("源数据库连接成功")
            
            logger.info("正在连接目标数据库...")
            self.target_conn = pymysql.connect(**self.target_config.to_mysql_config())
            logger.info("目标数据库连接成功")
            return True
        except Exception as e:
            logger.error(f"数据库连接失败: {e}")
            return False
    
    def close(self):
        """关闭数据库连接"""
        if self.source_conn:
            self.source_conn.close()
            logger.info("源数据库连接已关闭")
        if self.target_conn:
            self.target_conn.close()
            logger.info("目标数据库连接已关闭")
    
    def get_source_tables(self) -> List[str]:
        """获取源数据库所有表名"""
        with self.source_conn.cursor() as cursor:
            cursor.execute("SHOW TABLES")
            return [list(row.values())[0] for row in cursor.fetchall()]
    
    def get_table_schema(self, table_name: str, conn) -> List[Dict]:
        """获取表结构"""
        with conn.cursor() as cursor:
            cursor.execute(f"DESCRIBE `{table_name}`")
            return cursor.fetchall()
    
    def fetch_data(self, table_name: str, batch_size: int = 1000) -> List[Dict]:
        """分批从源表获取数据"""
        offset = 0
        while True:
            with self.source_conn.cursor() as cursor:
                sql = f"SELECT * FROM `{table_name}` LIMIT %s OFFSET %s"
                cursor.execute(sql, (batch_size, offset))
                rows = cursor.fetchall()
                if not rows:
                    break
                yield rows
                offset += batch_size
    
    def transform_record(self, record: Dict, table_name: str) -> Dict:
        """
        数据转换规则
        子类可重写此方法实现自定义转换逻辑
        """
        transformed = {}
        for key, value in record.items():
            # 字段名转换（小写+下划线）
            new_key = key.lower().replace(' ', '_')
            
            # 时间字段标准化
            if isinstance(value, datetime):
                transformed[new_key] = value
            # NULL值处理
            elif value is None:
                transformed[new_key] = None
            else:
                transformed[new_key] = value
        return transformed
    
    def insert_batch(self, table_name: str, records: List[Dict]) -> int:
        """批量插入数据到目标表"""
        if not records:
            return 0
        
        try:
            # 构建插入语句
            columns = list(records[0].keys())
            placeholders = ', '.join(['%s'] * len(columns))
            column_names = ', '.join([f'`{c}`' for c in columns])
            
            sql = f"INSERT INTO `{table_name}` ({column_names}) VALUES ({placeholders})"
            
            with self.target_conn.cursor() as cursor:
                values = [[record.get(col) for col in columns] for record in records]
                cursor.executemany(sql, values)
                self.target_conn.commit()
                return len(records)
        except Exception as e:
            self.target_conn.rollback()
            logger.error(f"批量插入失败: {e}")
            raise
    
    def migrate_table(self, table_name: str, batch_size: int = 1000, 
                      transform_fn: Optional[Callable] = None) -> Dict:
        """
        迁移单个表
        
        Args:
            table_name: 表名
            batch_size: 批处理大小
            transform_fn: 自定义转换函数
        
        Returns:
            迁移统计信息
        """
        logger.info(f"开始迁移表: {table_name}")
        stats = {'table': table_name, 'migrated': 0, 'failed': 0, 'errors': []}
        
        try:
            transform = transform_fn or self.transform_record
            
            for batch in self.fetch_data(table_name, batch_size):
                transformed_batch = []
                for record in batch:
                    try:
                        transformed = transform(record, table_name)
                        transformed_batch.append(transformed)
                    except Exception as e:
                        stats['failed'] += 1
                        stats['errors'].append(f"转换失败: {e}")
                
                if transformed_batch:
                    try:
                        inserted = self.insert_batch(table_name, transformed_batch)
                        stats['migrated'] += inserted
                        logger.info(f"已迁移 {stats['migrated']} 条记录")
                    except Exception as e:
                        stats['failed'] += len(transformed_batch)
                        stats['errors'].append(f"插入失败: {e}")
            
            self.migration_stats['tables_processed'] += 1
            self.migration_stats['records_migrated'] += stats['migrated']
            self.migration_stats['records_failed'] += stats['failed']
            
            logger.info(f"表 {table_name} 迁移完成: 成功 {stats['migrated']}, 失败 {stats['failed']}")
            return stats
            
        except Exception as e:
            logger.error(f"迁移表 {table_name} 失败: {e}")
            stats['errors'].append(str(e))
            return stats
    
    def migrate_all(self, tables: Optional[List[str]] = None, 
                    exclude_tables: Optional[List[str]] = None,
                    batch_size: int = 1000) -> Dict:
        """
        迁移所有表
        
        Args:
            tables: 指定要迁移的表，None则迁移所有
            exclude_tables: 要排除的表
            batch_size: 批处理大小
        """
        if tables is None:
            tables = self.get_source_tables()
        
        if exclude_tables:
            tables = [t for t in tables if t not in exclude_tables]
        
        logger.info(f"开始迁移 {len(tables)} 个表: {tables}")
        
        results = []
        for table in tables:
            result = self.migrate_table(table, batch_size)
            results.append(result)
        
        return {
            'summary': self.migration_stats,
            'details': results
        }


class MongoDBMigrator:
    """MongoDB数据迁移器"""
    
    def __init__(self, source_uri: str, target_uri: str, 
                 source_db: str, target_db: str):
        self.source_client = None
        self.target_client = None
        self.source_uri = source_uri
        self.target_uri = target_uri
        self.source_db_name = source_db
        self.target_db_name = target_db
    
    def connect(self):
        """连接MongoDB"""
        try:
            self.source_client = pymongo.MongoClient(self.source_uri)
            self.target_client = pymongo.MongoClient(self.target_uri)
            logger.info("MongoDB连接成功")
            return True
        except Exception as e:
            logger.error(f"MongoDB连接失败: {e}")
            return False
    
    def close(self):
        """关闭连接"""
        if self.source_client:
            self.source_client.close()
        if self.target_client:
            self.target_client.close()
    
    def migrate_collection(self, collection_name: str, 
                           batch_size: int = 1000) -> Dict:
        """迁移单个集合"""
        source_coll = self.source_client[self.source_db_name][collection_name]
        target_coll = self.target_client[self.target_db_name][collection_name]
        
        total = source_coll.count_documents({})
        migrated = 0
        failed = 0
        
        logger.info(f"开始迁移集合 {collection_name}，共 {total} 条文档")
        
        cursor = source_coll.find({}, batch_size=batch_size)
        batch = []
        
        for doc in cursor:
            try:
                # 移除_id避免冲突，或转换ObjectId
                if '_id' in doc:
                    doc['_id'] = str(doc['_id'])
                batch.append(doc)
                
                if len(batch) >= batch_size:
                    try:
                        target_coll.insert_many(batch, ordered=False)
                        migrated += len(batch)
                    except Exception as e:
                        failed += len(batch)
                        logger.error(f"批量插入失败: {e}")
                    batch = []
                    logger.info(f"已迁移 {migrated}/{total}")
            except Exception as e:
                failed += 1
                logger.error(f"文档处理失败: {e}")
        
        # 处理剩余批次
        if batch:
            try:
                target_coll.insert_many(batch, ordered=False)
                migrated += len(batch)
            except Exception as e:
                failed += len(batch)
        
        logger.info(f"集合 {collection_name} 迁移完成: 成功 {migrated}, 失败 {failed}")
        return {'collection': collection_name, 'migrated': migrated, 'failed': failed}


def main():
    parser = argparse.ArgumentParser(description='数据迁移工具')
    parser.add_argument('--source-host', default='localhost', help='源数据库主机')
    parser.add_argument('--source-port', type=int, default=3306, help='源数据库端口')
    parser.add_argument('--source-user', required=True, help='源数据库用户名')
    parser.add_argument('--source-password', required=True, help='源数据库密码')
    parser.add_argument('--source-db', required=True, help='源数据库名')
    
    parser.add_argument('--target-host', default='localhost', help='目标数据库主机')
    parser.add_argument('--target-port', type=int, default=3306, help='目标数据库端口')
    parser.add_argument('--target-user', required=True, help='目标数据库用户名')
    parser.add_argument('--target-password', required=True, help='目标数据库密码')
    parser.add_argument('--target-db', required=True, help='目标数据库名')
    
    parser.add_argument('--tables', nargs='+', help='指定迁移的表')
    parser.add_argument('--exclude', nargs='+', help='排除的表')
    parser.add_argument('--batch-size', type=int, default=1000, help='批处理大小')
    parser.add_argument('--dry-run', action='store_true', help='试运行模式')
    
    args = parser.parse_args()
    
    # 构建配置
    source_config = DatabaseConfig(
        host=args.source_host,
        port=args.source_port,
        user=args.source_user,
        password=args.source_password,
        database=args.source_db
    )
    
    target_config = DatabaseConfig(
        host=args.target_host,
        port=args.target_port,
        user=args.target_user,
        password=args.target_password,
        database=args.target_db
    )
    
    # 执行迁移
    migrator = DataMigrator(source_config, target_config)
    
    if not migrator.connect():
        sys.exit(1)
    
    try:
        if args.dry_run:
            logger.info("【试运行模式】仅显示要迁移的表，不执行实际迁移")
            tables = migrator.get_source_tables()
            logger.info(f"将迁移以下表: {tables}")
        else:
            result = migrator.migrate_all(
                tables=args.tables,
                exclude_tables=args.exclude,
                batch_size=args.batch_size
            )
            logger.info(f"迁移完成! 统计: {json.dumps(result['summary'], indent=2, default=str)}")
    finally:
        migrator.close()


if __name__ == '__main__':
    main()