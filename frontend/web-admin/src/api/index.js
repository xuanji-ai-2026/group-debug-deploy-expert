/**
 * index.js - API 统一导出文件
 * 功能：整合所有 API 模块，统一导出方便调用
 * 
 * @author 王强 (EMP-FE-002)
 * @date 2024-01
 */

// 先引入 request
import { request } from '../utils/request'

// 引入各模块 API
import tenantApi from './tenant'
import billingApi from './billing'
import systemApi from './system'
import * as userApi from './user'
import * as contentApi from './content'
import * as crawlApi from './crawl'
import * as nurturingApi from './nurturing'
import * as riskApi from './risk'

// 导出各模块 API
export const tenant = tenantApi
export const billing = billingApi
export const system = systemApi
export const user = userApi
export const content = contentApi
export const crawl = crawlApi
export const nurturing = nurturingApi
export const risk = riskApi
export { request }
export default request
