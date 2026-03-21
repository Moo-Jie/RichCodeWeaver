// 测试 parseBatchContent 的 JSON 边界解析逻辑
const testContent = `计划创建一个基于 Vue3 的计算器网站。{"data":"开始","type":"ai_response"}{"id":"call_1a0188afd186499f8462f5","name":"creatAndWrite","arguments":"{\\"relativeFilePath\\":\\"index.html\\"}","type":"tool_request"}{"id":"call_1a0188afd186499f8462f5","name":"creatAndWrite","arguments":"{\\"relativeFilePath\\":\\"index.html\\"}","result":"文件创建成功","type":"tool_executed"}项目完成。`

function findJsonObjectEnd(content, start) {
  let depth = 0
  let inString = false
  let escapeNext = false

  for (let i = start; i < content.length; i++) {
    const char = content[i]

    if (escapeNext) {
      escapeNext = false
      continue
    }

    if (char === '\\' && inString) {
      escapeNext = true
      continue
    }

    if (char === '"') {
      inString = !inString
      continue
    }

    if (inString) {
      continue
    }

    if (char === '{') {
      depth++
    } else if (char === '}') {
      depth--
      if (depth === 0) {
        return i
      }
    }
  }

  return -1
}

// 模拟提取 JSON 对象
let i = 0
const results = []
while (i < testContent.length) {
  const jsonStart = testContent.indexOf('{', i)
  if (jsonStart === -1) {
    results.push({ type: 'text', content: testContent.substring(i) })
    break
  }

  if (jsonStart > i) {
    results.push({ type: 'text', content: testContent.substring(i, jsonStart) })
  }

  const jsonEnd = findJsonObjectEnd(testContent, jsonStart)
  if (jsonEnd === -1) {
    results.push({ type: 'text', content: testContent.substring(jsonStart) })
    break
  }

  const chunk = testContent.substring(jsonStart, jsonEnd + 1)
  try {
    const parsed = JSON.parse(chunk)
    results.push({ type: 'json', content: chunk, parsed })
  } catch (e) {
    results.push({ type: 'invalid_json', content: chunk, error: e.message })
  }

  i = jsonEnd + 1
}

console.log('解析结果：')
results.forEach((r, idx) => {
  console.log(`\n[${idx}] ${r.type}:`)
  if (r.type === 'json') {
    console.log('  原始:', r.content.substring(0, 80) + (r.content.length > 80 ? '...' : ''))
    console.log('  解析:', JSON.stringify(r.parsed, null, 2))
  } else {
    console.log('  内容:', r.content)
  }
})
