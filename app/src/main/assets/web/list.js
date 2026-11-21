// 消息列表渲染：与官方网页同样的结构（用户气泡 + 回答 + 思考 + 来源 + 动作条）
(function () {
  var messages = []
  var stickToBottom = true

  var ICONS = {
    copy: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="9" y="9" width="11" height="11" rx="2.5"/><path d="M15 5.5A2.5 2.5 0 0 0 12.5 3H6.5A2.5 2.5 0 0 0 4 5.5v6A2.5 2.5 0 0 0 6.5 14"/></svg>',
    speaker: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 9.5h3.2L12 5.5v13L7.2 14.5H4z" stroke-linejoin="round"/><path d="M15.5 8.5a5 5 0 0 1 0 7"/><path d="M18 6a8.5 8.5 0 0 1 0 12"/></svg>',
    share: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7"/><path d="M12 16V4"/><path d="M8 8l4-4 4 4" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    refresh: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M20 12a8 8 0 1 1-2.4-5.7"/><path d="M20 4v4.5h-4.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    left: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14.5 6l-6 6 6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    right: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9.5 6l6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    down: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 9.5l6 6 6-6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
  }

  function escapeHtml(s) {
    return String(s == null ? '' : s).replace(/[&<>"]/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]
    })
  }

  function fmt(n) {
    if (!n) return '0'
    if (n < 1000) return String(n)
    if (n < 10000) return (n / 1000).toFixed(1) + 'k'
    return Math.round(n / 1000) + 'k'
  }

  function reasoningHtml(m) {
    if (!m.reasoning && m.status !== 'reasoning') return ''
    var head = m.status === 'reasoning' ? '正在思考' : '已思考（用时 ' + (m.reasoningSeconds || 1) + ' 秒）'
    return (
      '<div class="reasoning">' +
      '<div class="reasoning-head ' + (m.reasoningExpanded ? 'open' : '') + '">' +
      '<span class="arrow">' + ICONS.right + '</span>' +
      '<span>' + escapeHtml(head) + '</span>' +
      '</div>' +
      '<div class="reasoning-body ' + (m.reasoningExpanded ? 'show' : '') + '">' +
      (window.DP ? DP.renderMarkdown(m.reasoning || '') : '') +
      '</div>' +
      '</div>'
    )
  }

  function actionsHtml(m, sessionTokens) {
    var parts = []
    if (m.branchCount > 1) {
      parts.push(
        '<span class="branch">' +
          '<span class="chev' + (m.branchIndex <= 1 ? ' disabled' : '') + '">' + ICONS.left + '</span>' +
          '<span>' + m.branchIndex + '/' + m.branchCount + '</span>' +
          '<span class="chev' + (m.branchIndex >= m.branchCount ? ' disabled' : '') + '">' + ICONS.right + '</span>' +
        '</span>'
      )
    }
    parts.push('<span class="icon" data-action="copy">' + ICONS.copy + '</span>')
    parts.push('<span class="icon" data-action="speak">' + ICONS.speaker + '</span>')
    parts.push('<span class="icon" data-action="share">' + ICONS.share + '</span>')
    if (m.usage) {
      parts.push(
        '<span class="tokens">▼ ' + fmt(m.usage.completion) + ' tok · 本会话 ' + fmt(sessionTokens) + '</span>'
      )
    }
    parts.push('<span class="spacer"></span>')
    parts.push('<span class="icon retry" data-action="regenerate">' + ICONS.refresh + '</span>')
    return '<div class="actions">' + parts.join('') + '</div>'
  }

  function renderMessage(m, sessionTokens) {
    var id = escapeHtml(m.id)
    if (m.role === 'user') {
      return '<div class="msg user" data-id="' + id + '"><div class="bubble">' + escapeHtml(m.text) + '</div></div>'
    }
    var streaming = m.status === 'streaming' || m.status === 'reasoning'
    var body = ''
    if (m.status === 'failed') {
      body = '<div class="hint">' + escapeHtml(m.errorHint || '请检查网络后重试') + '</div>'
    } else if (m.text) {
      body = window.DP ? DP.renderMarkdown(m.text) : escapeHtml(m.text)
    }
    var sources =
      m.sourcesCount > 0
        ? '<div class="sources">🌐 ' + m.sourcesCount + ' 个网页</div>'
        : ''
    var hint = ''
    if (m.status === 'aborted') hint = '<div class="hint">已停止生成</div>'
    if (m.status === 'completed' && m.finishReason === 'length') {
      hint = '<div class="hint">回答达到长度上限，已截断</div>'
    }
    return (
      '<div class="msg assistant' + (streaming ? ' streaming' : '') + '" data-id="' + id + '">' +
      reasoningHtml(m) +
      '<div class="body">' + body + '</div>' +
      sources +
      hint +
      (m.status === 'completed' || m.status === 'aborted' ? actionsHtml(m, sessionTokens) : '') +
      '</div>'
    )
  }

  function render() {
    var list = document.getElementById('list')
    if (!list) return
    var sessionTokens = Number(list.dataset.sessionTokens || 0)
    var html = ''
    for (var i = 0; i < messages.length; i++) html += renderMessage(messages[i], sessionTokens)
    list.innerHTML = html
    if (stickToBottom) list.scrollTop = list.scrollHeight
    if (window.AndroidHost && window.AndroidHost.onContentChanged) window.AndroidHost.onContentChanged()
  }

  window.setTheme = function (theme) {
    if (!theme) return
    var s = document.documentElement.style
    s.setProperty('--fg', theme.fg)
    s.setProperty('--fg2', theme.fg2)
    s.setProperty('--outline', theme.outline)
    s.setProperty('--link', theme.link)
    s.setProperty('--codeBg', theme.codeBg)
    s.setProperty('--bubble', theme.bubble)
    s.setProperty('--surface2', theme.surface2)
    document.body.classList.toggle('dark', !!theme.dark)
    document.body.classList.toggle('light', !theme.dark)
  }

  window.setSessionTokens = function (n) {
    document.getElementById('list').dataset.sessionTokens = String(n || 0)
  }

  window.setMessages = function (json) {
    try {
      var payload = typeof json === 'string' ? JSON.parse(json) : json
      messages = payload.messages || []
      document.getElementById('list').dataset.sessionTokens = String(payload.sessionTokens || 0)
    } catch (e) {
      messages = []
    }
    render()
  }

  window.scrollToBottom = function () {
    var list = document.getElementById('list')
    list.scrollTop = list.scrollHeight
    stickToBottom = true
  }

  document.addEventListener('DOMContentLoaded', function () {
    var list = document.getElementById('list')
    list.addEventListener('scroll', function () {
      stickToBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 24
    })
    // 思考折叠：本地交互，不打扰原生
    list.addEventListener('click', function (e) {
      var head = e.target.closest && e.target.closest('.reasoning-head')
      if (!head) return
      head.classList.toggle('open')
      var body = head.nextElementSibling
      if (body) body.classList.toggle('show')
    })
    if (window.AndroidHost && window.AndroidHost.onReady) window.AndroidHost.onReady()
  })
})()
