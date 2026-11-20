// DeepPort 消息渲染：markdown-it + KaTeX + highlight.js
(function () {
  var md = window.markdownit({
    html: false,
    linkify: true,
    breaks: false,
    highlight: function (str, lang) {
      if (lang && window.hljs && hljs.getLanguage(lang)) {
        try { return hljs.highlight(str, { language: lang, ignoreIllegals: true }).value; } catch (e) {}
      }
      return '';
    }
  });

  // 行内公式 $...$（排除 $$；跳过转义）
  md.inline.ruler.before('escape', 'math_inline', function (state, silent) {
    var start = state.pos;
    if (state.src[start] !== '$') return false;
    if (state.src[start + 1] === '$') return false;
    if (start > 0 && state.src[start - 1] === '\\') return false;
    var pos = start + 1;
    while (pos < state.posMax) {
      var ch = state.src[pos];
      if (ch === '\\') { pos += 2; continue; }
      if (ch === '$') break;
      if (ch === '\n') return false;
      pos++;
    }
    if (pos >= state.posMax || state.src[pos] !== '$') return false;
    var tex = state.src.slice(start + 1, pos);
    if (!tex.trim()) return false;
    if (!silent) {
      var token = state.push('math_inline', 'math', 0);
      token.content = tex;
    }
    state.pos = pos + 1;
    return true;
  });
  md.renderer.rules.math_inline = function (tokens, idx) {
    return '<span class="math-inline">' + window.katex.renderToString(tokens[idx].content, { throwOnError: false }) + '</span>';
  };

  // 块级公式 $$...$$
  md.block.ruler.before('fence', 'math_block', function (state, startLine, endLine, silent) {
    var pos = state.bMarks[startLine] + state.tShift[startLine];
    var max = state.eMarks[startLine];
    var line = state.src.slice(pos, max);
    if (line.indexOf('$$') !== 0) return false;
    var tex = line.slice(2);
    var closed = tex.indexOf('$$') >= 0;
    if (!closed) tex += '\n';
    var next = startLine;
    while (!closed && next + 1 < endLine) {
      next++;
      var p2 = state.bMarks[next] + state.tShift[next];
      var m2 = state.eMarks[next];
      var l2 = state.src.slice(p2, m2);
      var idx = l2.indexOf('$$');
      if (idx >= 0) { tex += l2.slice(0, idx); closed = true; } else { tex += l2 + '\n'; }
    }
    if (!closed) return false;
    if (!silent) {
      var token = state.push('math_block', 'math', 0);
      token.block = true;
      token.content = tex;
    }
    state.line = next + 1;
    return true;
  });
  md.renderer.rules.math_block = function (tokens, idx) {
    return '<div class="math-block">' + window.katex.renderToString(tokens[idx].content, { displayMode: true, throwOnError: false }) + '</div>';
  };

  function postProcess() {
    // 表格横向滚动容器
    document.querySelectorAll('#root table').forEach(function (t) {
      if (t.parentElement && t.parentElement.classList.contains('table-wrap')) return;
      var wrap = document.createElement('div');
      wrap.className = 'table-wrap';
      t.parentNode.insertBefore(wrap, t);
      wrap.appendChild(t);
    });
    // 代码高亮
    if (window.hljs) {
      document.querySelectorAll('#root pre code').forEach(function (el) {
        try { hljs.highlightElement(el); } catch (e) {}
      });
    }
  }

  function reportHeight() {
    var h = Math.ceil(document.getElementById('root').getBoundingClientRect().height);
    if (window.AndroidHost && window.AndroidHost.onHeight) window.AndroidHost.onHeight(h);
  }

  window.setTheme = function (theme) {
    var s = document.documentElement.style;
    if (!theme) return;
    s.setProperty('--fg', theme.fg);
    s.setProperty('--fg2', theme.fg2);
    s.setProperty('--outline', theme.outline);
    s.setProperty('--link', theme.link);
    s.setProperty('--codeBg', theme.codeBg);
    if (theme.bg) s.setProperty('--bg', theme.bg);
    document.body.style.background = 'transparent';
    reportHeight();
  };

  window.render = function (markdown) {
    var root = document.getElementById('root');
    root.innerHTML = md.render(markdown || '');
    postProcess();
    // 图片加载完、公式字体就绪后再报一次高度
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(reportHeight);
    setTimeout(reportHeight, 30);
    setTimeout(reportHeight, 300);
  };

  if (window.ResizeObserver) {
    new ResizeObserver(function () { reportHeight(); }).observe(document.getElementById('root'));
  }
})();
