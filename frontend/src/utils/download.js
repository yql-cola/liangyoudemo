import { assertDownloadBlob } from './request'

export async function downloadFile(response, fallbackName = 'export.xlsx') {
  const blob = response instanceof Blob ? response : response.data
  await assertDownloadBlob(blob)
  const headers = response.headers || {}
  const disposition = headers['content-disposition'] || headers['Content-Disposition'] || ''
  const utfName = disposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  const plainName = disposition.match(/filename\s*=\s*"?([^";]+)"?/i)
  let filename = fallbackName
  if (utfName) {
    try { filename = decodeURIComponent(utfName[1]) } catch (e) { filename = fallbackName }
  } else if (plainName) filename = plainName[1]
  filename = filename.replace(/[/\\]/g, '_')
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  try {
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
  } finally {
    link.remove()
    URL.revokeObjectURL(url)
  }
}
