/**
 * @fileoverview 本地存储工具
 * @description 封装 localStorage / sessionStorage 操作，提供类型安全的存取接口
 * @author EMP-FE-001 张婷
 */

/**
 * 存储类型枚举
 */
export const StorageType = {
  LOCAL: 'localStorage',
  SESSION: 'sessionStorage',
}

/**
 * 存储工具类
 */
class Storage {
  constructor(type) {
    this.storage = window[type]
    this.type = type
  }

  /**
   * 设置值（自动 JSON 序列化）
   * @param {string} key - 键名
   * @param {any} value - 值（会自动序列化为 JSON）
   */
  set(key, value) {
    try {
      const serialized = JSON.stringify(value)
      this.storage.setItem(key, serialized)
    } catch (error) {
      console.error(`[Storage] set error: ${key}`, error)
    }
  }

  /**
   * 获取值（自动 JSON 反序列化）
   * @param {string} key - 键名
   * @param {any} defaultValue - 默认值（当值不存在或解析失败时返回）
   * @returns {any} 返回反序列化后的值或默认值
   */
  get(key, defaultValue = null) {
    try {
      const item = this.storage.getItem(key)
      if (item === null) {
        return defaultValue
      }
      return JSON.parse(item)
    } catch (error) {
      console.warn(`[Storage] get parse error: ${key}`, error)
      return defaultValue
    }
  }

  /**
   * 移除指定键
   * @param {string} key - 键名
   */
  remove(key) {
    try {
      this.storage.removeItem(key)
    } catch (error) {
      console.error(`[Storage] remove error: ${key}`, error)
    }
  }

  /**
   * 清空存储
   */
  clear() {
    try {
      this.storage.clear()
    } catch (error) {
      console.error('[Storage] clear error', error)
    }
  }

  /**
   * 检查键是否存在
   * @param {string} key - 键名
   * @returns {boolean}
   */
  has(key) {
    return this.storage.getItem(key) !== null
  }

  /**
   * 获取存储大小
   * @returns {number} 键的数量
   */
  getSize() {
    return this.storage.length
  }

  /**
   * 获取所有键
   * @returns {string[]} 键名数组
   */
  keys() {
    return Object.keys(this.storage)
  }
}

// 创建实例（localStorage 和 sessionStorage）
export const localCache = new Storage(StorageType.LOCAL)
export const sessionCache = new Storage(StorageType.SESSION)

// 默认导出 localStorage 实例
export default localCache
