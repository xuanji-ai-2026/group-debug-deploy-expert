#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据同步脚本 - 跨数据库同步与增量同步
================================================
Author: 孙丽 (EMP-DATA-002)
Version: 1.0.0
"""

import os
import sys
import json
import logging
import argparse
import hashlib
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple, Set
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict
import threading
import time

import pymysql
from pymysql.cursors import DictCursor
import redis

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(f'sync_data_{datetime.now():%Y%m%d}.log')
    ]
)
logger = logging.getLogger(__name__)


class SyncConflictPolicy(Enum):
    """冲突处理策略"""
    SOURCE_WINS = "source_wins"      # 源端优先
    TARGET_WINS = "target_wins"      # 目标端优先
    NEWER_WINS = "newer_wins"        # 时间戳新的优先
    MANUAL = "manual"                # 手动处理
    SKIP = "skip"                    # 跳过


@dataclass
class SyncConfig:
    """同步配置"""
    source_table: str
    target_table: str
    primary_key: str = 'id'
    sync_fields: List[str] = field(default_factory=list)
    exclude_fields: List[str] = field(default_factory=list)
    timestamp_field: str = 'updated_at'
    soft_delete_field: str = 'is_deleted'
    conflict_policy: SyncConflictPolicy = SyncConflictPolicy.NEWER_WINS
    batch_size: int = 1000


@dataclass
class SyncStats:
    """同步统计"""
    records_inserted: int = 0
    records_updated: int = 0
    records_deleted: int = 0
    records_skipped: int = 0
    conflicts_resolved: int = 0
    errors: List[str] = field(default_factory=list)
    start_time: datetime = field(default_factory=datetime.now)
    end_time: Optional[datetime] = None
    
    @property
    def duration_seconds(self) -> float:
        end = self.end_time or datetime.now()
        return (end - self.start_time).total_seconds()
    
    @property
    def total_processed(self) -> int:
        return self.records_inserted + self.records_updated + self.records_deleted + self.records_skipped


class DataSynchronizer:
    """数据同步器"""
    
    def __init__(self, source_config: Dict, target_config: Dict,
                 redis_config: Optional[Dict] = None):
        self.source_config = source_config
        self.target_config = target_config
        self.redis_config = redis_config
        
        self.source_conn = None
        self.target_conn = None
        self.redis_client = None
        
        self._sync_state = {}
        self._lock = threading.Lock()
    
    def connect(self) -> bool:
        """建立所有连接"""
        try:
            logger.info("正在连接源数据库...")
            self.source_conn = pymysql.connect(**self.source_config)
            
            logger.info("正在连接目标数据库...")
            self.target_conn = pymysql.connect(**self.target_config)
            
            if self.redis_config:
                logger.info("正在连接Redis...")
                self.redis_client = redis.Redis(**self.redis_config)
                self.redis_client.ping()
            
            logger.info("所有连接建立成功")
            return True
            
        except Exception as e:
            logger.error(f"连接失败: {e}")
            return False
    
    def close(self):
        """关闭所有连接"""
        if self.source_conn:
            self.source_conn.close()
        if self.target_conn:
            self.target_conn.close()
        if self.redis_client:
            self.redis_client.close()
        logger.info("所有连接已关闭")
    
    def get_table_columns(self, conn, table_name: str) -> List[str]:
        """获取表的列名列表"""
        with conn.cursor() as cursor:
            cursor.execute(f"DESCRIBE `{table_name}`")
            return [row['Field'] for row in cursor.fetchall()]
    
    def get_last_sync_time(self, sync_key: str) -> Optional[datetime]:
        """获取上次同步时间"""
        if self.redis_client:
            timestamp = self.redis_client.get(f"sync:last:{sync_key}")
            if timestamp:
                return datetime.fromisoformat(timestamp.decode())
        
        # 从本地状态获取
        return self._sync_state.get(sync_key)
    
    def set_last_sync_time(self, sync_key: str, sync_time: datetime):
        """设置上次同步时间"""
        if self.redis_client:
            self.redis_client.set(f"sync:last:{sync_key}", sync_time.isoformat())
        
        self._sync_state[sync_key] = sync_time
    
    def compute_record_hash(self, record: Dict, fields: List[str]) -> str:
        """计算记录哈希用于变更检测"""
        content = ''.join([str(record.get(f, '')) for f in fields])
        return hashlib.md5(content.encode()).hexdigest()
    
    def fetch_source_data(self, config: SyncConfig,
                         last_sync_time: Optional[datetime] = None) -> List[Dict]:
        """从源端获取数据"""
        fields = config.sync_fields or self.get_table_columns(self.source_conn, config.source_table)
        fields = [f for f in fields if f not in config.exclude_fields]
        
        field_str = ', '.join([f'`{f}`' for f in fields])
        
        conditions = []
        params = []
        
        if last_sync_time and config.timestamp_field:
            conditions.append(f"`{config.timestamp_field}` >= %s")
            params.append(last_sync_time)
        
        where_clause = f"WHERE {' AND '.join(conditions)}" if conditions else ""
        
        sql = f"SELECT {field_str} FROM `{config.source_table}` {where_clause}"
        
        with self.source_conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()
    
    def fetch_target_data(self, config: SyncConfig) -> Dict[str, Dict]:
        """从目标端获取现有数据（以主键为索引）"""
        fields = config.sync_fields or self.get_table_columns(self.target_conn, config.target_table)
        fields = [f for f in fields if f not in config.exclude_fields]
        
        field_str = ', '.join([f'`{f}`' for f in fields])
        pk = config.primary_key
        
        sql = f"SELECT {field_str} FROM `{config.target_table}`"
        
        with self.target_conn.cursor() as cursor:
            cursor.execute(sql)
            rows = cursor.fetchall()
            return {str(row[pk]): row for row in rows}
    
    def resolve_conflict(self, source_record: Dict, target_record: Dict,
                        config: SyncConfig) -> Tuple[Dict, str]:
        """
        解决冲突
        
        Returns:
            (获胜记录, 冲突解决方式)
        """
        policy = config.conflict_policy
        
        if policy == SyncConflictPolicy.SOURCE_WINS:
            return source_record, "source_wins"
        
        elif policy == SyncConflictPolicy.TARGET_WINS:
            return target_record, "target_wins"
        
        elif policy == SyncConflictPolicy.NEWER_WINS:
            ts_field = config.timestamp_field
            source_ts = source_record.get(ts_field)
            target_ts = target_record.get(ts_field)
            
            if source_ts and target_ts:
                if source_ts > target_ts:
                    return source_record, "source_newer"
                else:
                    return target_record, "target_newer"
            return source_record, "source_default"
        
        elif policy == SyncConflictPolicy.SKIP:
            return None, "skipped"
        
        else:
            # MANUAL策略：记录冲突待处理
            return None, "manual_review"
    
    def insert_record(self, table_name: str, record: Dict) -> bool:
        """插入单条记录"""
        try:
            columns = list(record.keys())
            placeholders = ', '.join(['%s'] * len(columns))
            column_names = ', '.join([f'`{c}`' for c in columns])
            
            sql = f"INSERT INTO `{table_name}` ({column_names}) VALUES ({placeholders})"
            
            with self.target_conn.cursor() as cursor:
                cursor.execute(sql, list(record.values()))
                return True
        except Exception as e:
            logger.error(f"插入失败: {e}")
            return False
    
    def update_record(self, table_name: str, record: Dict, 
                     primary_key: str) -> bool:
        """更新单条记录"""
        try:
            pk_value = record.pop(primary_key, None)
            if not pk_value:
                return False
            
            columns = list(record.keys())
            set_clause = ', '.join([f'`{c}` = %s' for c in columns])
            
            sql = f"UPDATE `{table_name}` SET {set_clause} WHERE `{primary_key}` = %s"
            values = list(record.values()) + [pk_value]
            
            with self.target_conn.cursor() as cursor:
                cursor.execute(sql, values)
                return cursor.rowcount > 0
        except Exception as e:
            logger.error(f"更新失败: {e}")
            return False
    
    def delete_record(self, table_name: str, primary_key: str, 
                     pk_value: Any) -> bool:
        """删除单条记录"""
        try:
            sql = f"DELETE FROM `{table_name}` WHERE `{primary_key}` = %s"
            
            with self.target_conn.cursor() as cursor:
                cursor.execute(sql, [pk_value])
                return cursor.rowcount > 0
        except Exception as e:
            logger.error(f"删除失败: {e}")
            return False
    
    def sync_table(self, config: SyncConfig, 
                  incremental: bool = True) -> SyncStats:
        """
        同步单个表
        
        Args:
            config: 同步配置
            incremental: 是否增量同步
        """
        stats = SyncStats()
        sync_key = f"{config.source_table}:{config.target_table}"
        
        try:
            # 获取上次同步时间
            last_sync = None
            if incremental:
                last_sync = self.get_last_sync_time(sync_key)
                if last_sync:
                    logger.info(f"增量同步，上次同步时间: {last_sync}")
            
            # 获取源数据
            logger.info(f"从源表 {config.source_table} 获取数据...")
            source_data = self.fetch_source_data(config, last_sync)
            logger.info(f"源表数据: {len(source_data)} 条")
            
            if not source_data:
                logger.info("没有需要同步的数据")
                return stats
            
            # 获取目标数据
            logger.info(f"从目标表 {config.target_table} 获取数据...")
            target_data = self.fetch_target_data(config)
            logger.info(f"目标表数据: {len(target_data)} 条")
            
            # 对比并同步
            for record in source_data:
                pk_value = str(record.get(config.primary_key))
                target_record = target_data.get(pk_value)
                
                if not target_record:
                    # 新增记录
                    if self.insert_record(config.target_table, record.copy()):
                        stats.records_inserted += 1
                    else:
                        stats.errors.append(f"插入失败: {pk_value}")
                else:
                    # 记录存在，检查是否变化
                    fields = config.sync_fields or list(record.keys())
                    source_hash = self.compute_record_hash(record, fields)
                    target_hash = self.compute_record_hash(target_record, fields)
                    
                    if source_hash != target_hash:
                        # 有变化，需要处理冲突
                        winner, resolution = self.resolve_conflict(
                            record, target_record, config
                        )
                        
                        if winner is None:
                            stats.records_skipped += 1
                        elif winner == record:
                            # 源端胜出，执行更新
                            if self.update_record(config.target_table, record.copy(), config.primary_key):
                                stats.records_updated += 1
                                stats.conflicts_resolved += 1
                            else:
                                stats.errors.append(f"更新失败: {pk_value}")
                        else:
                            stats.records_skipped += 1
                            stats.conflicts_resolved += 1
                    else:
                        stats.records_skipped += 1
            
            # 处理软删除（目标端有但源端没有且标记为删除）
            if config.soft_delete_field:
                source_ids = {str(r.get(config.primary_key)) for r in source_data}
                for pk, record in target_data.items():
                    if pk not in source_ids and not record.get(config.soft_delete_field):
                        # 可选：标记为删除或物理删除
                        pass
            
            self.target_conn.commit()
            
            # 更新同步时间
            self.set_last_sync_time(sync_key, datetime.now())
            
        except Exception as e:
            self.target_conn.rollback()
            logger.error(f"同步失败: {e}")
            stats.errors.append(str(e))
        
        stats.end_time = datetime.now()
        return stats
    
    def sync_multiple(self, configs: List[SyncConfig],
                     parallel: bool = False) -> Dict[str, SyncStats]:
        """同步多个表"""
        results = {}
        
        for config in configs:
            logger.info(f"开始同步: {config.source_table} -> {config.target_table}")
            stats = self.sync_table(config)
            results[config.source_table] = stats
            
            logger.info(f"同步完成: 插入 {stats.records_inserted}, "
                       f"更新 {stats.records_updated}, "
                       f"跳过 {stats.records_skipped}, "
                       f"耗时 {stats.duration_seconds:.2f}s")
        
        return results


class RealTimeSync:
    """实时同步（基于binlog）"""
    
    def __init__(self, source_config: Dict, target_config: Dict):
        self.source_config = source_config
        self.target_config = target_config
        self.synchronizer = DataSynchronizer(source_config, target_config)
        self._running = False
    
    def start(self, configs: List[SyncConfig]):
        """启动实时同步"""
        self._running = True
        logger.info("启动实时同步服务...")
        
        # 简化的轮询实现（实际生产环境应使用binlog监听）
        while self._running:
            try:
                if not self.synchronizer.connect():
                    time.sleep(5)
                    continue
                
                for config in configs:
                    self.synchronizer.sync_table(config, incremental=True)
                
                self.synchronizer.close()
                time.sleep(10)  # 轮询间隔
                
            except KeyboardInterrupt:
                break
            except Exception as e:
                logger.error(f"实时同步错误: {e}")
                time.sleep(5)
    
    def stop(self):
        """停止实时同步"""
        self._running = False
        logger.info("实时同步服务已停止")


def main():
    parser = argparse.ArgumentParser(description='数据同步工具')
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
    
    parser.add_argument('--source-table', required=True, help='源表名')
    parser.add_argument('--target-table', required=True, help='目标表名')
    parser.add_argument('--primary-key', default='id', help='主键字段')
    parser.add_argument('--timestamp-field', default='updated_at', help='时间戳字段')
    
    parser.add_argument('--conflict-policy', 
                       choices=['source_wins', 'target_wins', 'newer_wins', 'skip'],
                       default='newer_wins', help='冲突处理策略')
    parser.add_argument('--full-sync', action='store_true', help='全量同步')
    parser.add_argument('--realtime', action='store_true', help='实时同步模式')
    
    args = parser.parse_args()
    
    source_config = {
        'host': args.source_host,
        'port': args.source_port,
        'user': args.source_user,
        'password': args.source_password,
        'database': args.source_db,
        'cursorclass': DictCursor,
        'charset': 'utf8mb4'
    }
    
    target_config = {
        'host': args.target_host,
        'port': args.target_port,
        'user': args.target_user,
        'password': args.target_password,
        'database': args.target_db,
        'cursorclass': DictCursor,
        'charset': 'utf8mb4'
    }
    
    policy_map = {
        'source_wins': SyncConflictPolicy.SOURCE_WINS,
        'target_wins': SyncConflictPolicy.TARGET_WINS,
        'newer_wins': SyncConflictPolicy.NEWER_WINS,
        'skip': SyncConflictPolicy.SKIP
    }
    
    sync_config = SyncConfig(
        source_table=args.source_table,
        target_table=args.target_table,
        primary_key=args.primary_key,
        timestamp_field=args.timestamp_field,
        conflict_policy=policy_map[args.conflict_policy]
    )
    
    if args.realtime:
        realtime_sync = RealTimeSync(source_config, target_config)
        try:
            realtime_sync.start([sync_config])
        except KeyboardInterrupt:
            realtime_sync.stop()
    else:
        synchronizer = DataSynchronizer(source_config, target_config)
        
        if not synchronizer.connect():
            sys.exit(1)
        
        try:
            stats = synchronizer.sync_table(sync_config, incremental=not args.full_sync)
            
            logger.info("=" * 50)
            logger.info("同步统计:")
            logger.info(f"  插入: {stats.records_inserted}")
            logger.info(f"  更新: {stats.records_updated}")
            logger.info(f"  删除: {stats.records_deleted}")
            logger.info(f"  跳过: {stats.records_skipped}")
            logger.info(f"  冲突解决: {stats.conflicts_resolved}")
            logger.info(f"  错误: {len(stats.errors)}")
            logger.info(f"  耗时: {stats.duration_seconds:.2f}秒")
            logger.info("=" * 50)
            
        finally:
            synchronizer.close()


if __name__ == '__main__':
    main()