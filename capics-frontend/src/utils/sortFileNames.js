export function sortFileNamesDesc(fileNames = []) {
  return [...fileNames].sort((left, right) =>
    String(right || '').localeCompare(String(left || ''), 'zh-CN', {
      numeric: true,
      sensitivity: 'base'
    })
  )
}

export function withSortedFileNames(response) {
  if (!Array.isArray(response?.data)) return response
  return {
    ...response,
    data: sortFileNamesDesc(response.data)
  }
}
