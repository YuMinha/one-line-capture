import { api } from './api.js'
import { toastError } from './toast.js'
import { currentMonth, dayLabel, formatWon, monthLabel, shiftMonth } from './format.js'

export function renderSummary(app) {
  app.innerHTML = `
    <div class="month-nav">
      <button type="button" id="prev" aria-label="이전 달">‹</button>
      <strong id="month-label"></strong>
      <button type="button" id="next" aria-label="다음 달">›</button>
    </div>
    <div class="totals">
      <div><span>총액</span><strong id="total">-</strong></div>
      <div><span>건수</span><strong id="count">-</strong></div>
    </div>
    <p id="status" class="status" role="status" aria-live="polite"></p>
    <table class="daily">
      <thead><tr><th>날짜</th><th>금액</th><th>건수</th></tr></thead>
      <tbody id="daily"></tbody>
    </table>
  `

  const label = app.querySelector('#month-label')
  const total = app.querySelector('#total')
  const count = app.querySelector('#count')
  const daily = app.querySelector('#daily')
  const status = app.querySelector('#status')

  let month = currentMonth()

  function row(cells) {
    const tr = document.createElement('tr')
    for (const [text, className] of cells) {
      const td = document.createElement('td')
      td.textContent = text
      if (className) td.className = className
      tr.append(td)
    }
    return tr
  }

  async function load() {
    label.textContent = monthLabel(month)
    status.className = 'status'
    status.textContent = '불러오는 중...'
    try {
      const summary = await api.expenseSummary(month)
      total.textContent = formatWon(summary.totalAmount)
      count.textContent = `${summary.count}건`
      daily.replaceChildren(...summary.dailyTotals.map((d) =>
        row([[dayLabel(d.date)], [formatWon(d.amount), 'num'], [`${d.count}`, 'num']])))
      // 차트는 넣지 않는다. 숫자와 표로도 "이번 달 얼마 썼지"는 답이 나온다 (spec.md §5)
      status.textContent = summary.count ? '' : '이 달에는 지출 기록이 없습니다'
    } catch (error) {
      status.className = 'status fail'
      status.textContent = '불러오지 못했습니다'
      toastError(error, '요약을 불러오지 못했습니다')
    }
  }

  app.querySelector('#prev').addEventListener('click', () => { month = shiftMonth(month, -1); load() })
  app.querySelector('#next').addEventListener('click', () => { month = shiftMonth(month, 1); load() })

  load()
}
