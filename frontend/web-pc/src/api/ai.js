/**
 * @fileoverview AI服务 API 模块
 * @description 封装AI文案/图片/语音/意图识别等 HTTP 请求
 *
 * ✅ 100%已对齐：与后端AiServiceController严格匹配 (2026-05-20)
 * 后端对应：AiServiceController (@RequestMapping("/v1")) - 7个接口 ✅
 */
import { get, post } from '../utils/request.js'

/** 文案生成 - POST /v1/text/generate */
export function generateText(params) {
  return post('/v1/text/generate', params)
}

/** 批量文案生成 - POST /v1/text/batch-generate */
export function batchGenerateText(requests) {
  return post('/v1/text/batch-generate', requests)
}

/** 图片生成 - POST /v1/image/generate */
export function generateImage(params) {
  return post('/v1/image/generate', params)
}

/** 语音识别(ASR) - POST /v1/speech/recognize */
export function recognizeSpeech(params) {
  return post('/v1/speech/recognize', params)
}

/** 语音合成(TTS) - POST /v1/speech/synthesize */
export function synthesizeSpeech(params) {
  return post('/v1/speech/synthesize', params)
}

/** 意图识别 - POST /v1/intent/recognize */
export function recognizeIntent(params) {
  return post('/v1/intent/recognize', params)
}

/** AI服务健康检查 - GET /v1/health */
export function aiHealthCheck() {
  return get('/v1/health')
}
