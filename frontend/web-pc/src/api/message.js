/**
 * @fileoverview 消息管理 API 模块
 * @description 封装私信发送、模板管理等 HTTP 请求
 *
 * �?100%已对齐：与后端MessageController严格匹配 (2026-05-20)
 * 后端对应：MessageController (@RequestMapping("/api/message")) - 5个接�?�? */
import { get, post } from '../utils/request.js'

/** 发送私�?- POST /message/send */
export function sendMessage(params) {
  return post('/message/send', params)
}

/** 批量发送私�?- POST /message/batch-send */
export function batchSendMessage(params) {
  return post('/message/batch-send', params)
}

/** 创建消息模板 - POST /message/template/create */
export function createMessageTemplate(params) {
  return post('/message/template/create', params)
}

/** AI生成消息模板 - POST /message/template/generate-ai */
export function generateAiTemplates(params) {
  return post('/message/template/generate-ai', params)
}

/** 获取消息模板列表 - GET /message/templates */
export function getMessageTemplates(platformCode, intentLevel) {
  const params = {}
  if (platformCode) params.platformCode = platformCode
  if (intentLevel) params.intentLevel = intentLevel
  return get('/message/templates', params)
}
