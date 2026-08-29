import { api, ApiError } from './api.js'
import { toastError } from './toast.js'
import { FORMS, TYPES } from './edit-form.js'

const escape = (value) => String(value ?? '').replace(/[&<>"]/g, (c) =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))

export function openEditSheet(id, { onChanged } = {}) {
  const dialog = document.createElement('dialog')
  dialog.className = 'sheet'
  document.body.append(dialog)

  const close = () => {
    dialog.close()
    dialog.remove()
  }

  function renderError(message) {
    const box = dialog.querySelector('#sheet-error')
    if (box) box.textContent = message
  }

  function render(item, type) {
    const form = FORMS[type]
    dialog.innerHTML = `
      <form method="dialog" id="sheet-form">
        <p class="raw">${escape(item.rawText)}</p>
        <div class="type-picker">
          ${TYPES.map(([value, label]) =>
            `<button type="button" data-type="${value}" class="${value === type ? 'on' : ''}">${label}</button>`).join('')}
        </div>
        ${form.fields(item).map((field) => `
          <label>
            <span>${field.label}</span>
            <input name="${field.name}" type="${field.type}" value="${escape(field.value)}" ${field.attrs ?? ''} />
          </label>`).join('')}
        <p id="sheet-error" class="status fail"></p>
        <div class="sheet-actions">
          <button type="button" id="sheet-delete" class="danger">삭제</button>
          <button type="button" id="sheet-cancel">닫기</button>
          <button type="submit" id="sheet-save" class="primary">저장</button>
        </div>
      </form>
    `

    dialog.querySelector('.type-picker').addEventListener('click', (event) => {
      const button = event.target.closest('button')
      // 타입만 바꿔 다시 그린다. 저장 전까지 서버에는 아무것도 보내지 않는다
      if (button) render(item, button.dataset.type)
    })

    dialog.querySelector('#sheet-cancel').addEventListener('click', close)

    dialog.querySelector('#sheet-delete').addEventListener('click', async () => {
      if (!confirm('삭제하면 되돌릴 수 없습니다. 지울까요?')) return
      try {
        await api.remove(id)
        onChanged?.()
        close()
      } catch (error) {
        renderError('삭제하지 못했습니다')
        toastError(error, '삭제하지 못했습니다')
      }
    })

    dialog.querySelector('#sheet-form').addEventListener('submit', async (event) => {
      event.preventDefault()
      const values = Object.fromEntries(new FormData(event.target))
      try {
        await api.update(id, { type, ...form.body(values) })
        onChanged?.()
        close()
      } catch (error) {
        renderError(error instanceof ApiError ? error.message : '저장하지 못했습니다')
        toastError(error, '저장하지 못했습니다')
      }
    })
  }

  dialog.addEventListener('close', () => dialog.remove())
  dialog.showModal()
  dialog.innerHTML = '<p class="status">불러오는 중...</p>'

  // 목록의 전체 탭은 상세를 주지 않으므로 단건 조회로 채운다
  api.get(id)
      .then((item) => render(item, item.type))
      .catch((error) => {
        dialog.innerHTML = `<p class="status fail">${escape(
          error instanceof ApiError ? error.message : '불러오지 못했습니다')}</p>`
      })
}
