/**
 * 用户身份选项
 */
export const identityOptions = [
  { value: 'individual', label: '个体' },
  { value: 'merchant', label: '商户' },
  { value: 'enterprise', label: '企业' }
]

/**
 * 行业领域选项
 */
export const industryOptions = [
  '互联网/IT',
  '教育/培训',
  '电商/零售',
  '医疗/健康',
  '餐饮/美食',
  '金融/财务',
  '设计/创意',
  '房地产/建筑',
  '法律/咨询',
  '旅游/酒店',
  '制造/工业',
  '物流/运输',
  '农业/环保',
  '媒体/传播',
  '体育/健身'
]

/**
 * 身份值转中文标签
 */
export const identityLabelMap: Record<string, string> = {
  individual: '个体',
  merchant: '商户',
  enterprise: '企业'
}
