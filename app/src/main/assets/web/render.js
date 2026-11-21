// DeepPort message renderer wrapper: DP.renderMarkdown (micromark + llm-math + KaTeX + Prism)
(function () {
  function reportHeight() {
    var root = document.getElementById('root')
    if (!root) return
    // 取三种测量的最大值再补 2px：只用量 rect 时，最后一个代码块的 padding/圆角
    // 会被算漏，WebView 高度偏小，底部圆角就被裁成直角、字母也会被切掉。
    var h = Math.ceil(
      Math.max(
        root.getBoundingClientRect().height,
        root.scrollHeight,
        document.body.scrollHeight
      ) + 2
    )
    if (window.AndroidHost && window.AndroidHost.onHeight) window.AndroidHost.onHeight(h)
  }

  // 给代码块套一个「语言 + 复制」的头，视觉上是一个完整的块
  function decorateCodeBlocks() {
    var blocks = document.querySelectorAll('#root pre.md-code-block')
    for (var i = 0; i < blocks.length; i++) {
      var pre = blocks[i]
      var prev = pre.previousElementSibling
      if (prev && prev.classList && prev.classList.contains('md-code-bar')) continue
      var code = pre.querySelector('code')
      var lang = ''
      if (code && code.className) {
        var m = code.className.match(/language-([\w+#.-]+)/)
        if (m) lang = m[1]
      }
      var bar = document.createElement('div')
      bar.className = 'md-code-bar'
      var label = document.createElement('span')
      label.className = 'md-code-lang'
      label.textContent = lang || 'text'
      var btn = document.createElement('button')
      btn.className = 'md-code-copy'
      btn.type = 'button'
      btn.textContent = '复制'
      ;(function (preEl, button) {
        button.addEventListener('click', function () {
          var text = preEl.innerText.replace(/\n$/, '')
          if (window.AndroidHost && window.AndroidHost.copy) window.AndroidHost.copy(text)
          button.textContent = '已复制'
          setTimeout(function () { button.textContent = '复制' }, 1200)
        })
      })(pre, btn)
      bar.appendChild(label)
      bar.appendChild(btn)
      pre.parentNode.insertBefore(bar, pre)
    }
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
      decorateCodeBlocks()
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
