import os
import sys
import pymysql
from pathlib import Path

class DatabaseInitializer:
    def __init__(self, host, port, user, password, charset='utf8mb4'):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.charset = charset
        self.connection = None
        self.current_database = None

    def connect(self):
        """连接到数据库"""
        try:
            self.connection = pymysql.connect(
                host=self.host,
                port=self.port,
                user=self.user,
                password=self.password,
                charset=self.charset,
                cursorclass=pymysql.cursors.DictCursor,
                autocommit=True
            )
            print(f"✓ 成功连接到数据库 {self.host}:{self.port}")
            return True
        except pymysql.Error as e:
            print(f"✗ 数据库连接失败: {e}")
            return False

    def reconnect(self):
        """重新连接数据库"""
        if self.connection:
            self.connection.close()
        return self.connect()

    def execute_sql_file(self, file_path):
        """执行单个SQL文件"""
        print(f"\n正在执行: {file_path.name}")
        print("-" * 60)

        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                sql_content = file.read()

            if not sql_content.strip():
                print(f"  文件为空，跳过")
                return True

            # 使用更简单的分割方式：按行读取并拼接
            statements = self.split_sql_by_line(sql_content)
            success_count = 0
            error_count = 0

            current_db = None

            with self.connection.cursor() as cursor:
                for i, statement in enumerate(statements):
                    statement = statement.strip()
                    if not statement or statement.startswith('--'):
                        continue

                    # 检查是否是CREATE DATABASE语句
                    if statement.upper().startswith('CREATE DATABASE'):
                        print(f"  创建数据库...")
                        try:
                            cursor.execute(statement)
                            success_count += 1
                            # 提取数据库名
                            parts = statement.split('`')
                            if len(parts) >= 2:
                                db_name = parts[1]
                                current_db = db_name
                                print(f"  ✓ 数据库 '{db_name}' 创建成功")

                            # 创建后切换到该数据库
                            print(f"  切换到数据库: {current_db}")
                            cursor.execute(f"USE `{current_db}`")
                            print(f"  ✓ 已切换到数据库 '{current_db}'")

                        except pymysql.Error as e:
                            if 'already exists' in str(e).lower():
                                print(f"  ⚠ 数据库已存在")
                            else:
                                print(f"  ⚠ 创建数据库失败: {str(e)[:60]}")
                                error_count += 1

                    # 检查是否是USE语句
                    elif statement.upper().startswith('USE '):
                        db_name = statement.split()[1].strip('`;')
                        print(f"  切换到数据库: {db_name}")
                        try:
                            cursor.execute(statement)
                            current_db = db_name
                            self.current_database = db_name
                            success_count += 1
                            print(f"  ✓ 已切换到数据库 '{db_name}'")
                        except pymysql.Error as e:
                            print(f"  ⚠ 切换数据库失败: {str(e)[:60]}")
                            error_count += 1

                    # 其他SQL语句
                    else:
                        if current_db is None:
                            print(f"  ⚠ 未选择数据库，无法执行: {statement[:60]}...")
                            error_count += 1
                            continue

                        try:
                            cursor.execute(statement)
                            success_count += 1
                        except pymysql.Error as e:
                            error_count += 1
                            error_msg = str(e).lower()
                            if 'already exists' in error_msg:
                                table_match = statement.lower().split('table')
                                if len(table_match) >= 2:
                                    table_name = table_match[1].split()[0].strip('`(')
                                    print(f"  ⚠ 表 '{table_name}' 已存在，跳过")
                            elif 'unknown database' in error_msg:
                                print(f"  ⚠ 数据库不存在: {str(e)[:50]}")
                            elif 'no database selected' in error_msg:
                                print(f"  ⚠ 未选择数据库: {str(e)[:50]}")
                            else:
                                print(f"  ⚠ 执行失败: {str(e)[:60]}")

            print(f"  ✓ 成功执行 {success_count} 条语句", end="")
            if error_count > 0:
                print(f", {error_count} 条有警告")
            else:
                print()

            return True

        except FileNotFoundError:
            print(f"  ✗ 文件不存在: {file_path}")
            return False
        except Exception as e:
            print(f"  ✗ 执行失败: {e}")
            import traceback
            traceback.print_exc()
            return False

    def split_sql_by_line(self, sql_content):
        """按行分割SQL语句，处理跨行语句"""
        statements = []
        current_statement = []

        for line in sql_content.split('\n'):
            stripped = line.strip()

            # 跳过注释行
            if stripped.startswith('--'):
                continue

            current_statement.append(line)

            # 如果行尾有分号，说明语句结束
            if stripped.endswith(';'):
                statement = '\n'.join(current_statement)
                statements.append(statement)
                current_statement = []

        # 处理最后一个语句（可能没有分号）
        if current_statement:
            statement = '\n'.join(current_statement).strip()
            if statement and not statement.startswith('--'):
                statements.append(statement)

        return statements

    def close(self):
        """关闭数据库连接"""
        if self.connection:
            self.connection.close()
            print("\n✓ 数据库连接已关闭")


def get_sql_files(sql_dir):
    """获取SQL文件列表"""
    sql_files = []

    priority_order = ['init-all.sql', 'yuemo_user.sql', 'yuemo_product.sql',
                      'yuemo_order.sql', 'yuemo_payment.sql', 'yuemo_cart.sql',
                      'yuemo_promotion.sql']

    sql_dir_path = Path(sql_dir)

    for filename in priority_order:
        file_path = sql_dir_path / filename
        if file_path.exists():
            sql_files.append(file_path)

    for file_path in sql_dir_path.glob('*.sql'):
        if file_path not in sql_files:
            sql_files.append(file_path)

    return sql_files


def main():
    print("=" * 60)
    print("月魔商城 - 数据库初始化工具")
    print("=" * 60)

    db_config = {
        'host': '192.168.1.55',
        'port': 3306,
        'user': 'root',
        'password': 'jiangjing'
    }

    sql_directory = Path(__file__).parent.absolute()

    print(f"\n数据库配置:")
    print(f"  主机: {db_config['host']}")
    print(f"  端口: {db_config['port']}")
    print(f"  用户: {db_config['user']}")
    print(f"  SQL目录: {sql_directory}")
    print()

    initializer = DatabaseInitializer(**db_config)

    if not initializer.connect():
        sys.exit(1)

    sql_files = get_sql_files(sql_directory)

    if not sql_files:
        print("✗ 未找到SQL文件")
        initializer.close()
        sys.exit(1)

    print(f"\n找到 {len(sql_files)} 个SQL文件:")
    for i, file_path in enumerate(sql_files, 1):
        print(f"  {i}. {file_path.name}")

    print("\n" + "=" * 60)
    print("开始初始化数据库...")
    print("=" * 60)

    success_count = 0
    for file_path in sql_files:
        if initializer.execute_sql_file(file_path):
            success_count += 1

    print("\n" + "=" * 60)
    print("初始化完成!")
    print(f"  成功处理 {success_count}/{len(sql_files)} 个文件")
    print("=" * 60)

    print("\n✓ 数据库初始化完成！")
    print("\n已创建的数据库和表:")
    print("  1. yuemo_mall - 主数据库")
    print("     └─ 包含所有模块的表（开发环境用）")
    print("  2. yuemo_user - 用户数据库")
    print("     └─ yu_user, yu_address")
    print("  3. yuemo_product - 商品数据库")
    print("     └─ yu_product, yu_category")
    print("  4. yuemo_order - 订单数据库")
    print("     └─ yu_order, yu_order_item")
    print("  5. yuemo_payment - 支付数据库")
    print("     └─ yu_payment")
    print("  6. yuemo_cart - 购物车数据库")
    print("     └─ yu_cart_item")
    print("  7. yuemo_promotion - 促销数据库")
    print("     └─ yu_coupon, yu_user_coupon")

    initializer.close()


if __name__ == '__main__':
    main()
