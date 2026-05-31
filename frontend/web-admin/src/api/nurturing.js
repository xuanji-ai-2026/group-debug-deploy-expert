import { get, post, put, del } from '@/utils/request'

export function getNurturingStrategyList(params = {}) {
  return get('/nurturing/strategies', params)
}

export function getNurturingStrategyDetail(strategyId) {
  return get(`/nurturing/strategy/${strategyId}`)
}

export function createNurturingStrategy(params) {
  return post('/nurturing/strategy', params)
}

export function updateNurturingStrategy(strategyId, params) {
  return put(`/nurturing/strategy/${strategyId}`, params)
}

export function deleteNurturingStrategy(strategyId) {
  return del(`/nurturing/strategy/${strategyId}`)
}

export function toggleNurturingStatus(strategyId, status) {
  return put(`/nurturing/strategy/${strategyId}/status`, { status })
}

export function startNurturingStrategy(strategyId) {
  return post(`/nurturing/strategy/${strategyId}/start`)
}

export function stopNurturingStrategy(strategyId) {
  return post(`/nurturing/strategy/${strategyId}/stop`)
}

export function getNurturingProgress(strategyId) {
  return get(`/nurturing/strategy/${strategyId}/progress`)
}

export function getAccountNurturingStatus(accountId) {
  return get(`/nurturing/account/${accountId}/status`)
}

export function getNurturingTemplates() {
  return get('/nurturing/templates')
}
