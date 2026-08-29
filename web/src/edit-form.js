import { fromKstInput, toKstInput } from './format.js'

// 타입별로 어떤 입력을 보여주고, 무엇을 PATCH 본문으로 만들지만 정의한다.
// 타입이 늘면 여기 한 줄이 늘고 나머지는 그대로다
export const FORMS = {
  EXPENSE: {
    fields: (item) => [
      { name: 'amount', label: '금액', type: 'number', value: item.expense?.amount ?? 0, attrs: 'min="0" step="1" required' },
      { name: 'merchant', label: '항목', type: 'text', value: item.expense?.merchant ?? '', attrs: 'maxlength="100"' },
      { name: 'spentAt', label: '지출일', type: 'date', value: item.expense?.spentAt ?? '', attrs: 'required' },
    ],
    // PATCH는 전체 교체다. 빈 값을 안 채워 보내면 그 필드가 null로 덮인다
    body: (values) => ({
      expense: {
        amount: Number(values.amount),
        merchant: values.merchant || null,
        spentAt: values.spentAt,
      },
    }),
  },
  TODO: {
    fields: (item) => [
      { name: 'title', label: '제목', type: 'text', value: item.todo?.title ?? item.rawText, attrs: 'maxlength="200" required' },
      { name: 'dueAt', label: '마감', type: 'datetime-local', value: toKstInput(item.todo?.dueAt) },
    ],
    body: (values) => ({
      todo: { title: values.title, dueAt: fromKstInput(values.dueAt) },
    }),
  },
  LINK: {
    fields: (item) => [
      { name: 'url', label: 'URL', type: 'url', value: item.link?.url ?? '', attrs: 'maxlength="1000" required' },
      { name: 'note', label: '메모', type: 'text', value: item.link?.note ?? '', attrs: 'maxlength="300"' },
    ],
    body: (values) => ({
      link: { url: values.url, note: values.note || null },
    }),
  },
}

export const TYPES = [['EXPENSE', '지출'], ['TODO', '할일'], ['LINK', '링크']]
