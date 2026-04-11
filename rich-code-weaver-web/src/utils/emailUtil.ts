const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,24}$/

const SUSPICIOUS_DOMAIN_MAP: Record<string, string> = {
  'outlook.co': 'outlook.com',
  'hotmail.co': 'hotmail.com',
  'gamil.com': 'gmail.com',
  'gmail.con': 'gmail.com',
  'hotmial.com': 'hotmail.com',
  'yaho.com': 'yahoo.com',
  'qq.con': 'qq.com',
  '163.con': '163.com',
  '126.con': '126.com'
}

export type EmailValidationResult = {
  valid: boolean
  normalizedEmail: string
  message?: string
}

export const normalizeEmail = (email?: string) => (email ?? '').trim().toLowerCase()

export const validateEmail = (email?: string): EmailValidationResult => {
  const normalizedEmail = normalizeEmail(email)
  if (!normalizedEmail) {
    return {
      valid: false,
      normalizedEmail,
      message: '请输入邮箱'
    }
  }

  if (!EMAIL_REGEX.test(normalizedEmail)) {
    return {
      valid: false,
      normalizedEmail,
      message: '邮箱格式不正确'
    }
  }

  const atIndex = normalizedEmail.indexOf('@')
  if (atIndex <= 0 || atIndex !== normalizedEmail.lastIndexOf('@')) {
    return {
      valid: false,
      normalizedEmail,
      message: '邮箱格式不正确'
    }
  }

  const localPart = normalizedEmail.slice(0, atIndex)
  const domain = normalizedEmail.slice(atIndex + 1)

  if (!isValidLocalPart(localPart) || !isValidDomain(domain)) {
    return {
      valid: false,
      normalizedEmail,
      message: '邮箱格式不正确'
    }
  }

  const suggestedDomain = SUSPICIOUS_DOMAIN_MAP[domain]
  if (suggestedDomain) {
    return {
      valid: false,
      normalizedEmail,
      message: `邮箱域名看起来有误，你是否想输入 ${localPart}@${suggestedDomain}`
    }
  }

  return {
    valid: true,
    normalizedEmail
  }
}

export const isValidEmail = (email?: string) => validateEmail(email).valid

const isValidLocalPart = (localPart: string) => {
  if (!localPart || localPart.length > 64) {
    return false
  }
  return !localPart.startsWith('.') && !localPart.endsWith('.') && !localPart.includes('..')
}

const isValidDomain = (domain: string) => {
  if (!domain || domain.length > 253 || domain.includes('..')) {
    return false
  }

  const labels = domain.split('.')
  if (labels.length < 2) {
    return false
  }

  const labelRegex = /^[a-z0-9-]+$/
  for (const label of labels) {
    if (!label || label.length > 63 || !labelRegex.test(label)) {
      return false
    }
    if (label.startsWith('-') || label.endsWith('-')) {
      return false
    }
  }

  const topLevelDomain = labels[labels.length - 1]
  return /^[a-z]{2,24}$/.test(topLevelDomain)
}
