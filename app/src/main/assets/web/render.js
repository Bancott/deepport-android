// DeepPort message renderer wrapper: DP.renderMarkdown (micromark + llm-math + KaTeX + Prism)
(function () {
  function reportHeight() {
    var root = document.getElementById('root')
    if (!root) return
    var h = Math.ceil(root.getBoundingClientRect().height)
    if (window.AndroidHost && window.AndroidHost.onHeight) window.AndroidHost.onHeight(h)
  }

  window.setTheme = function (theme) {
    if (!theme) return
    var s = document.documentElement.style
    s.setProperty('--fg', theme.fg)
    s.setProperty('--fg2', theme.fg2)
    s.setProperty('--outline', theme.outline)
    s.setProperty('--link', theme.link)
    s.setProperty('--codeBg', theme.codeBg)
    document.body.classList.toggle('dark', !!theme.dark)
    document.body.classList.toggle('light', !theme.dark)
    reportHeight()
  }

  window.render = function (markdown) {
    var root = document.getElementById('root')
    try {
      root.innerHTML = window.DP && window.DP.renderMarkdown ? window.DP.renderMarkdown(markdown || '') : ''
    } catch (e) {
      root.textContent = markdown || ''
    }
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(reportHeight)
    setTimeout(reportHeight, 30)
    setTimeout(reportHeight, 300)
  }

  if (window.ResizeObserver) {
    new ResizeObserver(function () { reportHeight() }).observe(document.getElementById('root'))
  }
})()
