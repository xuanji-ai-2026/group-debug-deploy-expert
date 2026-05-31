#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试数据生成脚本 - 生成模拟业务数据
================================================
Author: 孙丽 (EMP-DATA-002)
Version: 1.0.0
"""

import os
import sys
import json
import logging
import argparse
import random
import hashlib
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass
from decimal import Decimal

import pymysql
from pymysql.cursors import DictCursor
from faker import Faker

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(f'mock_data_{datetime.now():%Y%m%d}.log')
    ]
)
logger = logging.getLogger(__name__)

# 初始化Faker
fake = Faker(['zh_CN', 'en_US'])
Faker.seed(42)


@dataclass
class TenantConfig:
    """租户配置"""
    count: int = 10
    plan_types: List[str] = None
    
    def __post_init__(self):
        if self.plan_types is None:
            self.plan_types = ['free', 'basic', 'pro', 'enterprise']


@dataclass
class UserConfig:
    """用户配置"""
    count: int = 100
    roles: List[str] = None
    
    def __post_init__(self):
        if self.roles is None:
            self.roles = ['admin', 'manager', 'operator', 'viewer']


@dataclass
class LeadConfig:
    """商机配置"""
    count: int = 500
    statuses: List[str] = None
    sources: List[str] = None
    
    def __post_init__(self):
        if self.statuses is None:
            self.statuses = ['new', 'contacted', 'qualified', 'proposal', 'negotiation', 'won', 'lost']
        if self.sources is None:
            self.sources = ['website', 'referral', 'social_media', 'cold_call', 'email', 'event', 'partner']


class MockDataGenerator:
    """测试数据生成器"""
    
    INDUSTRIES = [
        '互联网', '金融', '制造业', '零售', '医疗', '教育', '房地产', '物流',
        '餐饮', '旅游', '广告', '咨询', '法律', '会计', '建筑', '能源'
    ]
    
    COMPANY_SUFFIXES = ['科技', '网络', '信息', '软件', '智能', '数据', '云', '互联']
    
    CONTENT_TITLES = [
        'AI技术如何改变行业未来', '数字化转型最佳实践', '智能客服系统评测',
        '2026年营销趋势预测', '客户成功案例分析', 'SaaS产品选型指南',
        '企业效率提升秘籍', '数据驱动决策方法论', '自动化营销实战',
        '私域流量运营策略', 'B2B销售增长秘籍', '产品迭代经验分享'
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
        self.generated_ids = {
            'tenants': [],
            'users': [],
            'leads': [],
            'contents': []
        }
    
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
    
    def _generate_tenant_code(self, name: str) -> str:
        """生成租户编码"""
        prefix = ''.join([c[0] for c in name[:3] if c]).upper()
        suffix = hashlib.md5(name.encode()).hexdigest()[:6].upper()
        return f"T{prefix}{suffix}"
    
    def generate_tenants(self, config: TenantConfig) -> List[Dict]:
        """生成租户数据"""
        logger.info(f"开始生成 {config.count} 个租户...")
        tenants = []
        
        for i in range(config.count):
            company_name = fake.company()
            suffix = random.choice(self.COMPANY_SUFFIXES)
            if suffix not in company_name:
                company_name += suffix
            
            tenant = {
                'id': f"tnt_{i+1:06d}",
                'code': self._generate_tenant_code(company_name),
                'name': company_name,
                'plan_type': random.choice(config.plan_types),
                'status': random.choice(['active', 'active', 'active', 'suspended', 'inactive']),
                'industry': random.choice(self.INDUSTRIES),
                'contact_name': fake.name(),
                'contact_phone': fake.phone_number(),
                'contact_email': fake.company_email(),
                'max_users': random.choice([5, 10, 20, 50, 100, 200]),
                'storage_limit_gb': random.choice([10, 50, 100, 500, 1000]),
                'created_at': fake.date_time_between(start_date='-2y', end_date='now'),
                'expire_at': fake.date_time_between(start_date='+1M', end_date='+2y')
            }
            tenants.append(tenant)
        
        logger.info(f"租户数据生成完成")
        return tenants
    
    def generate_users(self, config: UserConfig, 
                       tenant_ids: List[str]) -> List[Dict]:
        """生成用户数据"""
        logger.info(f"开始生成 {config.count} 个用户...")
        users = []
        
        for i in range(config.count):
            tenant_id = random.choice(tenant_ids) if tenant_ids else None
            first_name = fake.first_name()
            last_name = fake.last_name()
            
            user = {
                'id': f"usr_{i+1:08d}",
                'tenant_id': tenant_id,
                'username': f"user_{i+1:06d}",
                'email': fake.email(),
                'phone': fake.phone_number(),
                'real_name': f"{last_name}{first_name}",
                'role': random.choice(config.roles),
                'status': random.choice(['active', 'active', 'active', 'inactive']),
                'avatar_url': f"https://api.dicebear.com/7.x/avataaars/svg?seed={i}",
                'department': fake.job(),
                'position': random.choice(['经理', '专员', '总监', '主管', '工程师']),
                'last_login_at': fake.date_time_between(start_date='-30d', end_date='now'),
                'created_at': fake.date_time_between(start_date='-1y', end_date='now')
            }
            users.append(user)
        
        logger.info(f"用户数据生成完成")
        return users
    
    def generate_leads(self, config: LeadConfig,
                       tenant_ids: List[str],
                       user_ids: List[str]) -> List[Dict]:
        """生成商机数据"""
        logger.info(f"开始生成 {config.count} 个商机...")
        leads = []
        
        for i in range(config.count):
            tenant_id = random.choice(tenant_ids) if tenant_ids else None
            owner_id = random.choice(user_ids) if user_ids else None
            
            # 生成时间线
            created_at = fake.date_time_between(start_date='-6M', end_date='now')
            status = random.choice(config.statuses)
            
            contacted_at = None
            qualified_at = None
            closed_at = None
            
            if status in ['contacted', 'qualified', 'proposal', 'negotiation', 'won', 'lost']:
                contacted_at = created_at + timedelta(days=random.randint(1, 7))
            if status in ['qualified', 'proposal', 'negotiation', 'won', 'lost']:
                qualified_at = contacted_at + timedelta(days=random.randint(1, 14))
            if status in ['won', 'lost']:
                closed_at = qualified_at + timedelta(days=random.randint(7, 60))
            
            # 预估金额
            estimated_value = random.choice([
                5000, 10000, 20000, 50000, 100000, 
                200000, 500000, 1000000
            ])
            
            lead = {
                'id': f"ld_{i+1:09d}",
                'tenant_id': tenant_id,
                'owner_id': owner_id,
                'source': random.choice(config.sources),
                'status': status,
                'priority': random.choice(['low', 'medium', 'high', 'urgent']),
                'company_name': fake.company(),
                'contact_name': fake.name(),
                'contact_phone': fake.phone_number(),
                'contact_email': fake.email(),
                'contact_title': fake.job(),
                'estimated_value': estimated_value,
                'actual_value': estimated_value if status == 'won' else None,
                'probability': random.choice([10, 20, 30, 50, 70, 90]),
                'notes': fake.text(max_nb_chars=200),
                'tags': ','.join(random.sample(['hot', 'vip', 'follow-up', 'key-account', 'partner'], k=random.randint(0, 3))),
                'created_at': created_at,
                'contacted_at': contacted_at,
                'qualified_at': qualified_at,
                'closed_at': closed_at,
                'updated_at': fake.date_time_between(start_date=created_at, end_date='now')
            }
            leads.append(lead)
        
        logger.info(f"商机数据生成完成")
        return leads
    
    def generate_contents(self, count: int = 200,
                          tenant_ids: List[str] = None,
                          user_ids: List[str] = None) -> List[Dict]:
        """生成内容数据"""
        logger.info(f"开始生成 {count} 条内容...")
        contents = []
        content_types = ['article', 'video', 'image', 'document', 'template']
        
        for i in range(count):
            content_type = random.choice(content_types)
            created_at = fake.date_time_between(start_date='-1y', end_date='now')
            
            content = {
                'id': f"cnt_{i+1:09d}",
                'tenant_id': random.choice(tenant_ids) if tenant_ids else None,
                'author_id': random.choice(user_ids) if user_ids else None,
                'type': content_type,
                'title': random.choice(self.CONTENT_TITLES) + f" - {i+1}",
                'summary': fake.text(max_nb_chars=100),
                'content': fake.text(max_nb_chars=2000) if content_type == 'article' else None,
                'url': fake.url() if content_type in ['video', 'document'] else None,
                'file_size': random.randint(1000, 10000000) if content_type in ['document', 'image'] else None,
                'status': random.choice(['draft', 'published', 'archived']),
                'view_count': random.randint(0, 10000),
                'like_count': random.randint(0, 1000),
                'share_count': random.randint(0, 500),
                'tags': ','.join(random.sample(['营销', '销售', 'AI', '数字化', '增长', '案例'], k=random.randint(1, 3))),
                'is_template': content_type == 'template',
                'created_at': created_at,
                'published_at': fake.date_time_between(start_date=created_at, end_date='now') if random.random() > 0.3 else None,
                'updated_at': fake.date_time_between(start_date=created_at, end_date='now')
            }
            contents.append(content)
        
        logger.info(f"内容数据生成完成")
        return contents
    
    def insert_data(self, table_name: str, records: List[Dict], 
                    batch_size: int = 1000) -> int:
        """批量插入数据"""
        if not records:
            return 0
        
        total_inserted = 0
        
        try:
            columns = list(records[0].keys())
            placeholders = ', '.join(['%s'] * len(columns))
            column_names = ', '.join([f'`{c}`' for c in columns])
            
            sql = f"INSERT INTO `{table_name}` ({column_names}) VALUES ({placeholders})"
            
            for i in range(0, len(records), batch_size):
                batch = records[i:i+batch_size]
                values = [[record.get(col) for col in columns] for record in batch]
                
                with self.conn.cursor() as cursor:
                    cursor.executemany(sql, values)
                    self.conn.commit()
                    total_inserted += cursor.rowcount
                    logger.info(f"已插入 {total_inserted}/{len(records)} 条记录到 {table_name}")
            
            return total_inserted
            
        except Exception as e:
            self.conn.rollback()
            logger.error(f"插入数据到 {table_name} 失败: {e}")
            raise
    
    def generate_and_insert(self, tenant_config: TenantConfig = None,
                            user_config: UserConfig = None,
                            lead_config: LeadConfig = None,
                            content_count: int = 200,
                            dry_run: bool = False) -> Dict:
        """生成并插入所有数据"""
        tenant_config = tenant_config or TenantConfig()
        user_config = user_config or UserConfig()
        lead_config = lead_config or LeadConfig()
        
        stats = {
            'tenants': 0,
            'users': 0,
            'leads': 0,
            'contents': 0
        }
        
        # 生成租户
        tenants = self.generate_tenants(tenant_config)
        if not dry_run:
            stats['tenants'] = self.insert_data('tenants', tenants)
        self.generated_ids['tenants'] = [t['id'] for t in tenants]
        
        # 生成用户
        users = self.generate_users(user_config, self.generated_ids['tenants'])
        if not dry_run:
            stats['users'] = self.insert_data('users', users)
        self.generated_ids['users'] = [u['id'] for u in users]
        
        # 生成商机
        leads = self.generate_leads(
            lead_config,
            self.generated_ids['tenants'],
            self.generated_ids['users']
        )
        if not dry_run:
            stats['leads'] = self.insert_data('leads', leads)
        self.generated_ids['leads'] = [l['id'] for l in leads]
        
        # 生成内容
        contents = self.generate_contents(
            content_count,
            self.generated_ids['tenants'],
            self.generated_ids['users']
        )
        if not dry_run:
            stats['contents'] = self.insert_data('contents', contents)
        self.generated_ids['contents'] = [c['id'] for c in contents]
        
        if dry_run:
            logger.info("【试运行模式】数据已生成但未插入")
            stats = {
                'tenants': len(tenants),
                'users': len(users),
                'leads': len(leads),
                'contents': len(contents)
            }
        
        return stats
    
    def export_to_json(self, output_dir: str = './mock_data'):
        """导出生成的数据到JSON文件"""
        os.makedirs(output_dir, exist_ok=True)
        
        for entity_type, ids in self.generated_ids.items():
            filepath = os.path.join(output_dir, f'{entity_type}_ids.json')
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(ids, f, ensure_ascii=False, indent=2)
            logger.info(f"已导出 {entity_type} ID列表: {filepath}")


def main():
    parser = argparse.ArgumentParser(description='测试数据生成工具')
    parser.add_argument('--host', default='localhost', help='数据库主机')
    parser.add_argument('--port', type=int, default=3306, help='数据库端口')
    parser.add_argument('--user', required=True, help='用户名')
    parser.add_argument('--password', required=True, help='密码')
    parser.add_argument('--database', required=True, help='数据库名')
    
    parser.add_argument('--tenants', type=int, default=10, help='租户数量')
    parser.add_argument('--users', type=int, default=100, help='用户数量')
    parser.add_argument('--leads', type=int, default=500, help='商机数量')
    parser.add_argument('--contents', type=int, default=200, help='内容数量')
    
    parser.add_argument('--batch-size', type=int, default=1000, help='批处理大小')
    parser.add_argument('--dry-run', action='store_true', help='试运行')
    parser.add_argument('--export-ids', action='store_true', help='导出ID列表')
    
    args = parser.parse_args()
    
    generator = MockDataGenerator(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database
    )
    
    if not generator.connect():
        sys.exit(1)
    
    try:
        tenant_config = TenantConfig(count=args.tenants)
        user_config = UserConfig(count=args.users)
        lead_config = LeadConfig(count=args.leads)
        
        stats = generator.generate_and_insert(
            tenant_config=tenant_config,
            user_config=user_config,
            lead_config=lead_config,
            content_count=args.contents,
            dry_run=args.dry_run
        )
        
        logger.info(f"数据生成统计: {json.dumps(stats, indent=2)}")
        
        if args.export_ids:
            generator.export_to_json()
    
    finally:
        generator.close()


if __name__ == '__main__':
    main()