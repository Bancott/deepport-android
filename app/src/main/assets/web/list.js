// 消息列表渲染：与官方网页同样的结构（用户气泡 + 回答 + 思考 + 来源 + 动作条）
(function () {
  var messages = []
  var stickToBottom = true

  /** 官方 ReadingBars 的单根条：height/y 同一套 spline、同一个 begin，逐帧严格同步。
      官方是 2 宽 / 2 间距，这里收细到 1.8 / 2.2，配合 18px 画框更精致 */
  function readingBar(x, begin) {
    var spline = '0.42 0 0.58 1;0.42 0 0.58 1'
    return (
      '<rect x="' + x + '" y="1" width="1.8" height="14" rx="0.9">' +
      '<animate attributeName="height" values="14;4;14" keyTimes="0;0.5;1" calcMode="spline"' +
      ' keySplines="' + spline + '" dur="1.6s" begin="' + begin + '" repeatCount="indefinite"/>' +
      '<animate attributeName="y" values="1;6;1" keyTimes="0;0.5;1" calcMode="spline"' +
      ' keySplines="' + spline + '" dur="1.6s" begin="' + begin + '" repeatCount="indefinite"/>' +
      '</rect>'
    )
  }

  var ICONS = {
    copy: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="9" y="9" width="11" height="11" rx="2.5"/><path d="M15 5.5A2.5 2.5 0 0 0 12.5 3H6.5A2.5 2.5 0 0 0 4 5.5v6A2.5 2.5 0 0 0 6.5 14"/></svg>',
    // 朗读按钮两态：空闲 = 官方轮廓喇叭；朗读中换四根竖条（官方 ReadingBars 参数）。
    // 竖条高度 14→4→14、1.6s 一轮、相邻条错开 0.4s，SMIL 同时改 height/y，圆角不变形。
    speaker:
      '<svg class="spk" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">' +
      '<path d="M12.441,2.426L12.617,2.457L12.771,2.503C12.87,2.54 12.966,2.588 13.056,2.646L13.186,2.741L13.315,2.864C13.588,3.161 13.657,3.527 13.685,3.756C13.717,4.032 13.716,4.38 13.716,4.733V19.397C13.716,19.728 13.717,20.06 13.686,20.325C13.654,20.583 13.571,21.003 13.21,21.311C13.051,21.447 12.865,21.546 12.664,21.603C12.207,21.732 11.811,21.57 11.579,21.453C11.341,21.332 11.066,21.146 10.79,20.962L5.62,17.516H4.658C4.303,17.516 3.965,17.517 3.685,17.487C3.388,17.455 3.051,17.378 2.741,17.153C2.58,17.036 2.438,16.894 2.321,16.733C2.096,16.423 2.02,16.086 1.987,15.79C1.957,15.51 1.958,15.171 1.958,14.816V9.185C1.958,8.83 1.957,8.491 1.987,8.212C2.019,7.915 2.096,7.578 2.321,7.268L2.414,7.151C2.511,7.038 2.62,6.935 2.741,6.847L2.859,6.77C3.136,6.604 3.425,6.542 3.685,6.514C3.965,6.484 4.303,6.485 4.658,6.485H6.361L10.705,3.227C10.987,3.015 11.265,2.805 11.505,2.667C11.705,2.551 12.039,2.386 12.441,2.426ZM11.785,4.668L7.202,8.105L6.962,8.285H4.658C4.263,8.285 4.039,8.286 3.879,8.303C3.837,8.308 3.809,8.314 3.791,8.318C3.787,8.336 3.782,8.364 3.777,8.405C3.76,8.565 3.759,8.79 3.759,9.185V14.816C3.759,15.211 3.76,15.435 3.777,15.595C3.782,15.636 3.787,15.664 3.791,15.682C3.809,15.686 3.837,15.693 3.879,15.698C4.039,15.715 4.263,15.716 4.658,15.716H6.165L6.392,15.867L11.789,19.465C11.833,19.495 11.875,19.521 11.914,19.547C11.914,19.5 11.916,19.45 11.916,19.397V4.733C11.916,4.676 11.914,4.622 11.914,4.571C11.873,4.601 11.831,4.634 11.785,4.668Z"/>' +
      '<path d="M19.144,5.002C23.008,8.866 23.008,15.133 19.144,18.998L17.87,17.724C21.032,14.562 21.032,9.437 17.87,6.275L19.144,5.002Z"/>' +
      '<path d="M16.207,7.938C18.45,10.181 18.45,13.819 16.207,16.062L14.934,14.789C16.474,13.248 16.474,10.752 14.934,9.212L16.207,7.938Z"/>' +
      '</svg>' +
      '<svg class="bars" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">' +
      readingBar(1.1, '0s') + readingBar(5.1, '-1.2s') +
      readingBar(9.1, '-0.8s') + readingBar(13.1, '-0.4s') +
      '</svg>',
    think: '<svg viewBox="0 0 16 16" fill="none" aria-hidden="true"><path d="M8.00192 6.64454C8.75026 6.64454 9.35732 7.25169 9.35739 8.00001C9.35739 8.74838 8.7503 9.35548 8.00192 9.35548C7.25367 9.35533 6.64743 8.74829 6.64743 8.00001C6.6475 7.25178 7.25371 6.64468 8.00192 6.64454Z" fill="currentColor"/><path fill-rule="evenodd" clip-rule="evenodd" d="M9.97165 1.29981C11.5853 0.718916 13.271 0.642197 14.3144 1.68555C15.3577 2.72902 15.2811 4.41466 14.7002 6.02833C14.4707 6.66561 14.1504 7.32937 13.75 8.00001C14.1504 8.67062 14.4707 9.33444 14.7002 9.97169C15.2811 11.5854 15.3578 13.271 14.3144 14.3145C13.271 15.3579 11.5854 15.2811 9.97165 14.7002C9.3344 14.4708 8.67059 14.1505 7.99997 13.75C7.32933 14.1505 6.66558 14.4708 6.02829 14.7002C4.41461 15.2811 2.72899 15.3578 1.68552 14.3145C0.642155 13.271 0.71887 11.5854 1.29977 9.97169C1.52915 9.33454 1.84865 8.67049 2.24899 8.00001C1.84866 7.32953 1.52915 6.66544 1.29977 6.02833C0.718852 4.41459 0.64207 2.729 1.68552 1.68555C2.72897 0.642112 4.41456 0.718887 6.02829 1.29981C6.66541 1.52918 7.32949 1.8487 7.99997 2.24903C8.67045 1.84869 9.33451 1.52919 9.97165 1.29981ZM12.9404 9.2129C12.4391 9.893 11.8616 10.5681 11.2148 11.2149C10.568 11.8616 9.89296 12.4391 9.21286 12.9404C9.62532 13.1579 10.0271 13.338 10.4121 13.4766C11.9146 14.0174 12.9172 13.8738 13.3955 13.3955C13.8737 12.9173 14.0174 11.9146 13.4765 10.4121C13.3379 10.0271 13.1578 9.62535 12.9404 9.2129ZM3.05856 9.2129C2.84121 9.62523 2.66197 10.0272 2.52341 10.4121C1.98252 11.9146 2.12627 12.9172 2.60446 13.3955C3.08278 13.8737 4.08544 14.0174 5.58786 13.4766C5.97264 13.338 6.37389 13.1577 6.7861 12.9404C6.10624 12.4393 5.43168 11.8614 4.78513 11.2149C4.13823 10.5679 3.55992 9.89313 3.05856 9.2129ZM7.99899 3.792C7.23179 4.31419 6.45306 4.95512 5.70407 5.70411C4.95509 6.45309 4.31415 7.23184 3.79196 7.99903C4.3143 8.76666 4.95471 9.54653 5.70407 10.2959C6.45309 11.0449 7.23271 11.6848 7.99997 12.207C8.76725 11.6848 9.54683 11.0449 10.2959 10.2959C11.0449 9.54686 11.6848 8.76729 12.207 8.00001C11.6848 7.23275 11.0449 6.45312 10.2959 5.70411C9.5465 4.95475 8.76662 4.31434 7.99899 3.792ZM5.58786 2.52344C4.08533 1.98255 3.08272 2.12625 2.60446 2.6045C2.12621 3.08275 1.98252 4.08536 2.52341 5.5879C2.66189 5.97253 2.8414 6.37409 3.05856 6.78614C3.55983 6.10611 4.1384 5.43189 4.78513 4.78516C5.43186 4.13843 6.10606 3.55987 6.7861 3.0586C6.37405 2.84144 5.97249 2.66192 5.58786 2.52344ZM13.3955 2.6045C12.9172 2.12631 11.9146 1.98257 10.4121 2.52344C10.0272 2.66201 9.62519 2.84125 9.21286 3.0586C9.8931 3.55996 10.5679 4.13827 11.2148 4.78516C11.8614 5.43172 12.4392 6.10627 12.9404 6.78614C13.1577 6.37393 13.338 5.97267 13.4765 5.5879C14.0174 4.08549 13.8736 3.08281 13.3955 2.6045Z" fill="currentColor"/></svg>',
    copy16: '<svg viewBox="0 0 16 16" fill="none" aria-hidden="true"><path d="M6.14929 4.02032C7.11197 4.02032 7.87983 4.02016 8.49597 4.07598C9.12128 4.13269 9.65792 4.25188 10.1415 4.53106C10.7202 4.8653 11.2008 5.3459 11.535 5.92462C11.8142 6.40818 11.9334 6.94481 11.9901 7.57012C12.0459 8.18625 12.0458 8.95419 12.0458 9.9168C12.0458 10.8795 12.0459 11.6473 11.9901 12.2635C11.9334 12.8888 11.8142 13.4254 11.535 13.909C11.2008 14.4877 10.7202 14.9683 10.1415 15.3025C9.65792 15.5817 9.12128 15.7009 8.49597 15.7576C7.87984 15.8134 7.11196 15.8133 6.14929 15.8133C5.18667 15.8133 4.41874 15.8134 3.80261 15.7576C3.1773 15.7009 2.64067 15.5817 2.1571 15.3025C1.5784 14.9683 1.09778 14.4877 0.76355 13.909C0.484366 13.4254 0.365184 12.8888 0.308472 12.2635C0.252649 11.6473 0.252808 10.8795 0.252808 9.9168C0.252808 8.95418 0.252664 8.18625 0.308472 7.57012C0.365184 6.94481 0.484366 6.40818 0.76355 5.92462C1.09777 5.34589 1.57839 4.86529 2.1571 4.53106C2.64067 4.25188 3.1773 4.13269 3.80261 4.07598C4.41874 4.02017 5.18666 4.02032 6.14929 4.02032ZM6.14929 5.37774C5.16181 5.37774 4.46634 5.37761 3.92566 5.42657C3.39434 5.47472 3.07859 5.56574 2.83582 5.70587C2.4632 5.92106 2.15354 6.2307 1.93835 6.60333C1.79823 6.8461 1.70721 7.16185 1.65906 7.69317C1.6101 8.23385 1.61023 8.92933 1.61023 9.9168C1.61023 10.9043 1.61009 11.5998 1.65906 12.1404C1.70721 12.6717 1.79823 12.9875 1.93835 13.2303C2.15356 13.6029 2.46321 13.9126 2.83582 14.1277C3.07859 14.2679 3.39434 14.3589 3.92566 14.407C4.46634 14.456 5.16182 14.4559 6.14929 14.4559C7.13682 14.4559 7.83224 14.456 8.37292 14.407C8.90425 14.3589 9.21999 14.2679 9.46277 14.1277C9.83535 13.9126 10.145 13.6029 10.3602 13.2303C10.5004 12.9875 10.5914 12.6717 10.6395 12.1404C10.6885 11.5998 10.6884 10.9043 10.6884 9.9168C10.6884 8.92934 10.6885 8.23384 10.6395 7.69317C10.5914 7.16185 10.5004 6.8461 10.3602 6.60333C10.1451 6.23071 9.83536 5.92107 9.46277 5.70587C9.21999 5.56574 8.90424 5.47472 8.37292 5.42657C7.83224 5.3776 7.13682 5.37774 6.14929 5.37774ZM9.80164 0.367975C10.7638 0.367975 11.5314 0.36788 12.1473 0.423639C12.7726 0.480307 13.3093 0.598759 13.7928 0.877741C14.3717 1.21192 14.8521 1.69355 15.1864 2.27227C15.4655 2.75574 15.5857 3.29164 15.6425 3.9168C15.6983 4.53301 15.6971 5.3016 15.6971 6.26446V7.82989C15.6971 8.29264 15.6989 8.58993 15.6649 8.84844C15.4668 10.3525 14.401 11.5738 12.9833 11.9988V10.5467C13.6973 10.1903 14.2105 9.49662 14.3192 8.67169C14.3387 8.52347 14.3407 8.3358 14.3407 7.82989V6.26446C14.3407 5.27706 14.3398 4.58149 14.2909 4.04083C14.2428 3.50968 14.1526 3.19372 14.0126 2.95098C13.7974 2.57849 13.4876 2.26869 13.1151 2.05352C12.8724 1.91347 12.5564 1.82237 12.0253 1.77423C11.4847 1.72528 10.7888 1.7254 9.80164 1.7254H7.71472C6.7562 1.72558 5.92665 2.27697 5.52332 3.07891H4.07019C4.54221 1.51132 5.9932 0.368186 7.71472 0.367975H9.80164Z" fill="currentColor"/></svg>',
    check16: '<svg viewBox="0 0 16 16" fill="none" aria-hidden="true"><path d="M15.0498 3.92579L8.49512 12.3818C8.25774 12.6881 8.04517 12.9645 7.84668 13.1689C7.63957 13.3823 7.38732 13.5841 7.04492 13.6719C6.86373 13.7183 6.6757 13.7346 6.48926 13.7197C6.13666 13.6915 5.8528 13.5355 5.6123 13.3604C5.38201 13.1926 5.12573 12.9567 4.83984 12.6953L1.03125 9.21289L1.96875 8.1875L5.77734 11.6699C6.08684 11.9529 6.27773 12.1249 6.43066 12.2363C6.50183 12.2882 6.54699 12.3135 6.57324 12.3252C6.58525 12.3305 6.59269 12.3322 6.5957 12.333C6.59802 12.3336 6.59961 12.334 6.59961 12.334C6.63317 12.3367 6.66758 12.3335 6.7002 12.3252C6.7002 12.3252 6.70211 12.3251 6.7041 12.3242C6.70698 12.3229 6.71348 12.319 6.72461 12.3115C6.74849 12.2956 6.78843 12.2642 6.84961 12.2012C6.98138 12.0654 7.13957 11.8628 7.39648 11.5313L13.9502 3.07422L15.0498 3.92579Z" fill="currentColor"/></svg>',
    chev14: '<svg viewBox="0 0 14 14" fill="none" aria-hidden="true"><path d="M11.8486 5.5L11.4238 5.92383L8.69727 8.65137C8.44157 8.90706 8.21562 9.13382 8.01172 9.29785C7.79912 9.46883 7.55595 9.61756 7.25 9.66602C7.08435 9.69222 6.91565 9.69222 6.75 9.66602C6.44405 9.61756 6.20088 9.46883 5.98828 9.29785C5.78438 9.13382 5.55843 8.90706 5.30273 8.65137L2.57617 5.92383L2.15137 5.5L3 4.65137L3.42383 5.07617L6.15137 7.80273C6.42595 8.07732 6.59876 8.24849 6.74023 8.3623C6.87291 8.46904 6.92272 8.47813 6.9375 8.48047C6.97895 8.48703 7.02105 8.48703 7.0625 8.48047C7.07728 8.47813 7.12709 8.46904 7.25977 8.3623C7.40124 8.24849 7.57405 8.07732 7.84863 7.80273L10.5762 5.07617L11 4.65137L11.8486 5.5Z" fill="currentColor"/></svg>',
    link14: '<svg viewBox="0 0 14 14" fill="none" aria-hidden="true"><path d="M3.2131 7.12714L4.44639 5.89384L5.22393 6.67138L3.99064 7.90468C3.40952 8.4858 3.40952 9.42831 3.99064 10.0094C4.57176 10.5905 5.51428 10.5905 6.09539 10.0094L7.32868 8.77613L8.10622 9.55367L6.87293 10.787C5.86224 11.7977 4.2238 11.7976 3.2131 10.787C2.2024 9.77627 2.2024 8.13783 3.2131 7.12714Z" fill="currentColor"/><path d="M7.12707 3.21304L5.89378 4.44633L6.67132 5.22387L7.90462 3.99058C8.48574 3.40946 9.42824 3.40946 10.0094 3.99058C10.5905 4.5717 10.5905 5.51421 10.0094 6.09533L8.77607 7.32862L9.55361 8.10616L10.7869 6.87287C11.7976 5.86218 11.7976 4.22374 10.7869 3.21304C9.77621 2.20234 8.13777 2.20234 7.12707 3.21304Z" fill="currentColor"/><path d="M8.79388 5.98339L5.98341 8.79387L5.20587 8.01633L8.01634 5.20585L8.79388 5.98339Z" fill="currentColor"/></svg>',
    // 原生 ic_share_outline_20（官方 2.8.0 提取的实心箭头），不是自绘的方框+上箭头
    share: '<svg viewBox="0 0 20 20" fill="currentColor"><path d="M9.741,2.914C9.742,2.111 10.659,1.693 11.262,2.153L11.377,2.257L17.909,9.068C18.41,9.59 18.41,10.415 17.909,10.937L11.377,17.744C10.785,18.361 9.741,17.941 9.741,17.086V13.284C8.646,13.337 6.167,13.827 3.01,16.759C2.776,16.975 2.46,17.01 2.208,16.914C1.948,16.813 1.714,16.551 1.715,16.193L1.724,15.791C1.802,13.758 2.409,11.388 3.703,9.462C4.991,7.544 6.972,6.06 9.741,5.832V2.914ZM11.241,6.53C11.241,6.973 10.879,7.3 10.473,7.301C7.898,7.307 6.117,8.56 4.948,10.299C4.106,11.553 3.591,13.055 3.359,14.493C7.312,11.425 10.191,11.754 10.602,11.823C11.06,11.9 11.241,12.303 11.241,12.582V15.719L16.728,10.002L11.241,4.281V6.53Z"/></svg>',
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
      '<span class="think">' + ICONS.think + '</span>' +
      '<span class="label">' + escapeHtml(head) + '</span>' +
      '<span class="chev">' + ICONS.chev14 + '</span>' +
      '</div>' +
      '<div class="reasoning-body ' + (m.reasoningExpanded ? 'show' : '') + '">' +
      '<div class="content"><span class="dot"></span>' +
      (window.DP ? DP.renderMarkdown(m.reasoning || '') : '') +
      '</div></div>' +
      '</div>'
    )
  }

  function actionsHtml(m, sessionTokens) {
    var parts = []
    if (m.branchCount > 1) {
      parts.push(
        '<span class="branch">' +
          '<span class="chev' + (m.branchIndex <= 1 ? ' disabled' : '') + '" data-action="branch-prev">' + ICONS.left + '</span>' +
          '<span>' + m.branchIndex + '/' + m.branchCount + '</span>' +
          '<span class="chev' + (m.branchIndex >= m.branchCount ? ' disabled' : '') + '" data-action="branch-next">' + ICONS.right + '</span>' +
        '</span>'
      )
    }
    parts.push('<span class="icon" data-action="copy">' + ICONS.copy + '</span>')
    parts.push('<span class="icon" data-action="speak">' + ICONS.speaker + '</span>')
    parts.push('<span class="icon" data-action="share">' + ICONS.share + '</span>')
    if (m.usage) {
      parts.push(
        '<span class="tokens">↑' + fmt(m.usage.prompt) + ' ↓' + fmt(m.usage.completion) +
          ' · 本会话 ' + fmt(sessionTokens) + '</span>'
      )
    }
    parts.push('<span class="spacer"></span>')
    parts.push('<span class="icon retry" data-action="regenerate">' + ICONS.refresh + '</span>')
    return '<div class="actions">' + parts.join('') + '</div>'
  }


  function decorateCodeBlocks() {
    var blocks = document.querySelectorAll('#list pre.md-code-block')
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
      btn.innerHTML = ICONS.copy16 + '<span>复制</span>'
      ;(function (preEl, button) {
        button.addEventListener('click', function () {
          var text = preEl.innerText.replace(/\n$/, '')
          if (window.AndroidHost && window.AndroidHost.copy) window.AndroidHost.copy(text)
          button.innerHTML = ICONS.check16 + '<span>已复制</span>'
          setTimeout(function () { button.innerHTML = ICONS.copy16 + '<span>复制</span>' }, 1200)
        })
      })(pre, btn)
      bar.appendChild(label)
      bar.appendChild(btn)
      pre.parentNode.insertBefore(bar, pre)
    }
  }

  /**
   * 回答末尾的「N 个网页」：官方 .summary 胶囊 —— 左侧最多 4 个站点图标（重叠 -4px），
   * 右侧文案，点击打开来源列表。数据来自原生下发的 m.sources。
   */
  function sourcesHtml(m) {
    if (!(m.sourcesCount > 0)) return ''
    var list = m.sources && m.sources.length ? m.sources : []
    var thumbs = ''
    var n = Math.min(4, list.length)
    for (var i = 0; i < n; i++) {
      var icon = list[i] && list[i].siteIcon ? list[i].siteIcon : ''
      // 兜底图标垫在底层：图标加载失败时 img 被移除，露出链接图形
      thumbs +=
        '<span class="src-logo">' + ICONS.link14 +
        (icon ? '<img src="' + escapeHtml(icon) + '" alt="" onerror="this.remove()">' : '') +
        '</span>'
    }
    if (!thumbs) thumbs = '<span class="src-logo">' + ICONS.link14 + '</span>'
    return (
      '<div class="sources" data-action="sources" role="button">' +
      '<span class="src-group">' + thumbs + '</span>' +
      '<span class="src-label">' + m.sourcesCount + ' 个网页</span>' +
      '</div>'
    )
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
    var sources = sourcesHtml(m)
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

  // 每条的 HTML 缓存：流式回答只重画变化的那一条，不整页重刷
  var lastHtml = []
  // 刚在本地点开/收起的消息：原生把同样的状态回显回来时不要再重画一次（重画会让整条闪一下）
  var pendingToggleId = ''
  // 展开/收起引起的高度变化不该把列表拽到底部（表现就是一跳 + 底部闪一下）
  var suppressStickOnce = false

  /** 去掉展开态 class，用来判断"这次变化是不是只有展开/收起" */
  function stripToggleClasses(html) {
    return html
      .replace(/class="reasoning-head open"/g, 'class="reasoning-head"')
      .replace(/class="reasoning-body show"/g, 'class="reasoning-body"')
  }

  function buildHtml() {
    var list = document.getElementById('list')
    var sessionTokens = Number(list.dataset.sessionTokens || 0)
    var out = []
    for (var i = 0; i < messages.length; i++) out.push(renderMessage(messages[i], sessionTokens))
    return out
  }

  function renderFull(list, html) {
    list.innerHTML = html.join('')
    lastHtml = html
    decorateCodeBlocks()
  }

  function render() {
    var list = document.getElementById('list')
    if (!list) return
    var next = buildHtml()
    // 条数变少、或对应位置的消息 id 变了（切换会话）→ 整页重建；否则逐条比对
    var rebuild = next.length < lastHtml.length
    if (!rebuild) {
      for (var i = 0; i < lastHtml.length; i++) {
        var el = list.children[i]
        if (!el || el.getAttribute('data-id') !== messages[i].id) {
          rebuild = true
          break
        }
      }
    }
    if (rebuild) {
      renderFull(list, next)
    } else {
      for (var j = 0; j < next.length; j++) {
        if (next[j] === lastHtml[j]) continue
        // 本地刚切换过展开态、且这一条的其余内容完全一致 → 保持现有 DOM，不重画
        if (
          pendingToggleId &&
          messages[j] &&
          messages[j].id === pendingToggleId &&
          stripToggleClasses(next[j]) === stripToggleClasses(lastHtml[j] || '')
        ) {
          continue
        }
        if (j < lastHtml.length && list.children[j]) {
          list.children[j].outerHTML = next[j]
        } else {
          list.insertAdjacentHTML('beforeend', next[j])
        }
      }
      lastHtml = next
      decorateCodeBlocks()
    }
    pendingToggleId = ''
    if (stickToBottom && !suppressStickOnce) list.scrollTop = list.scrollHeight
    suppressStickOnce = false
    // 增量替换会丢 class，重渲染后把朗读态补回去
    if (window.__readingId) window.setReadingMessage(window.__readingId)
    if (window.AndroidHost && window.AndroidHost.onContentChanged) window.AndroidHost.onContentChanged()
  }

  /** 「设置 → 字体大小」：整页字号都是 rem，改根字号即可让正文/思考/代码一起缩放 */
  function applyFontScale(fs) {
    if (!fs) return
    document.documentElement.style.fontSize = (16 * fs).toFixed(2) + 'px'
  }

  window.setTheme = function (theme) {
    if (!theme) return
    var s = document.documentElement.style
    applyFontScale(theme.fs)
    s.setProperty('--fg', theme.fg)
    s.setProperty('--fg2', theme.fg2)
    s.setProperty('--outline', theme.outline)
    s.setProperty('--link', theme.link)
    s.setProperty('--codeBg', theme.codeBg)
    s.setProperty('--bubble', theme.bubble)
    s.setProperty('--surface2', theme.surface2)
    if (theme.codeBlock) s.setProperty('--codeBlock', theme.codeBlock)
    if (theme.codeBanner) s.setProperty('--codeBanner', theme.codeBanner)
    if (theme.inlineCode) s.setProperty('--inlineCode', theme.inlineCode)
    s.setProperty('--thinkLine', theme.dark ? 'rgba(255,255,255,.14)' : '#e5e5e5')
    s.setProperty('--thinkDot', theme.dark ? '#adb2b8' : '#a2a4a6')
    document.body.classList.toggle('dark', !!theme.dark)
    document.body.classList.toggle('light', !theme.dark)
  }

  window.setSessionTokens = function (n) {
    document.getElementById('list').dataset.sessionTokens = String(n || 0)
  }

  window.setMessages = function (json) {
    try {
      var payload = typeof json === 'string' ? JSON.parse(json) : json
      // 首帧就按当前档位排：否则会先按 16px 画一遍再跳大，能看见一次抖动
      applyFontScale(payload.fs)
      messages = payload.messages || []
      document.getElementById('list').dataset.sessionTokens = String(payload.sessionTokens || 0)
    } catch (e) {
      console.error('setMessages parse failed: ' + e)
      messages = []
    }
    render()
  }

  window.scrollToBottom = function () {
    var list = document.getElementById('list')
    list.scrollTop = list.scrollHeight
    stickToBottom = true
  }


  // 右滑开历史：位移由原生用屏幕坐标算（网页里的 clientX 会被页面自身的平移抵消），
  // 网页只上报两件只有 DOM 知道的事：
  //   1) 这次按下落在哪个元素上、它还能不能朝某个方向横滑（代码块 / 表格 / 公式）；
  //   2) 当前有没有非空选区（有选区时把手势让给系统选择手柄）。
  function installGestureContext(list) {
    // 找到这次按下所在的"可横滑容器"：代码块 / 表格 / 公式块。
    // 代码块的语言栏（.md-code-bar）是 <pre> 的兄弟节点，点它也算在代码块里。
    function scrollerOf(node) {
      if (!node || !node.closest) return null
      var el = node.closest(
        'pre.md-code-block, .markdown-table-wrapper, .ds-markdown-math-block'
      )
      if (el) return el
      var bar = node.closest('.md-code-bar')
      if (bar && bar.nextElementSibling && bar.nextElementSibling.matches('pre.md-code-block')) {
        return bar.nextElementSibling
      }
      return null
    }

    function reportSelection() {
      var sel = window.getSelection && window.getSelection()
      var selected = !!(sel && sel.rangeCount > 0 && !sel.isCollapsed)
      if (window.AndroidHost && window.AndroidHost.setSelectionState) {
        window.AndroidHost.setSelectionState(selected)
      }
      // 选区矩形：给原生浮动菜单定位做兜底（Chromium 自带回调拿不到时用）
      if (window.AndroidHost && window.AndroidHost.setSelectionRect) {
        if (selected) {
          var r = sel.getRangeAt(0).getBoundingClientRect()
          window.AndroidHost.setSelectionRect(r.left, r.top, r.right, r.bottom)
        } else {
          window.AndroidHost.setSelectionRect(0, 0, 0, 0)
        }
      }
    }

    function reportContext(target) {
      var scroller = scrollerOf(target)
      var back = false
      var forward = false
      if (scroller) {
        var maxScroll = scroller.scrollWidth - scroller.clientWidth
        back = maxScroll > 0 && scroller.scrollLeft > 1
        forward = maxScroll > 0 && scroller.scrollLeft < maxScroll - 1
      }
      if (window.AndroidHost && window.AndroidHost.setGestureContext) {
        window.AndroidHost.setGestureContext(back, forward)
      }
      reportSelection()
    }

    document.addEventListener(
      'touchstart',
      function (e) {
        if (e.touches.length !== 1) return
        reportContext(e.target)
      },
      { capture: true, passive: true }
    )
    document.addEventListener('selectionchange', reportSelection)
  }

  // 动作条点击桥 / 用户消息长按菜单 / 选区朗读工具
  function installMessageInteractions(list) {
    list.addEventListener('click', function (e) {
      var el = e.target.closest && e.target.closest('[data-action]')
      if (!el) return
      var action = el.getAttribute('data-action')
      var msg = el.closest('.msg')
      var id = msg && msg.getAttribute('data-id')
      if (!action || !id) return
      if (window.AndroidHost && window.AndroidHost.messageAction) {
        window.AndroidHost.messageAction(id, action)
      }
    })

    // 用户气泡长按：弹原生菜单（复制 / 修改并创建分支）
    var timer = null
    var downX = 0
    var downY = 0
    function cancelPress() {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
    }

    list.addEventListener(
      'touchstart',
      function (e) {
        if (e.touches.length !== 1) return
        var bubble = e.target.closest && e.target.closest('.msg.user')
        if (!bubble) return
        var t = e.touches[0]
        downX = t.clientX
        downY = t.clientY
        var id = bubble.getAttribute('data-id')
        timer = setTimeout(function () {
          timer = null
          var r = bubble.getBoundingClientRect()
          if (window.AndroidHost && window.AndroidHost.userMessageMenu) {
            window.AndroidHost.userMessageMenu(id, r.left + r.width / 2, r.top)
          }
        }, 480)
      },
      { passive: true }
    )
    list.addEventListener(
      'touchmove',
      function (e) {
        if (!timer || e.touches.length !== 1) return
        var t = e.touches[0]
        if (Math.abs(t.clientX - downX) > 10 || Math.abs(t.clientY - downY) > 10) cancelPress()
      },
      { passive: true }
    )
    list.addEventListener('touchend', cancelPress, { passive: true })
    list.addEventListener('touchcancel', cancelPress, { passive: true })
    list.addEventListener('contextmenu', function (e) {
      if (e.target.closest && e.target.closest('.msg.user')) e.preventDefault()
    })
  }

  // 选区朗读：从选区起点读到这条消息结尾（系统菜单「朗读」用）
  window.__selectionTail = function () {
    var sel = window.getSelection && window.getSelection()
    if (!sel || sel.rangeCount === 0 || sel.isCollapsed) return ''
    var range = sel.getRangeAt(0)
    var start = range.startContainer
    var el = start && start.nodeType === 1 ? start : start && start.parentElement
    var msg = el && el.closest ? el.closest('.msg') : null
    if (!msg) return sel.toString()
    var full = msg.querySelector('.body') || msg.querySelector('.bubble') || msg
    try {
      var tail = document.createRange()
      tail.setStart(range.startContainer, range.startOffset)
      tail.setEnd(full, full.childNodes.length)
      return tail.toString()
    } catch (err) {
      return sel.toString()
    }
  }

  // 原生回到前台时问一句：页内条数和原生对不上，说明这份页面被系统回收过，需要重发。
  window.__needsMessages = function (count) {
    var list = document.getElementById('list')
    if (!list) return true
    return list.children.length !== count
  }

  window.clearSelection = function () {
    var sel = window.getSelection && window.getSelection()
    if (sel && sel.removeAllRanges) sel.removeAllRanges()
  }

  // 朗读状态：原生驱动，给对应消息的喇叭图标加上 .speaking（波形动效）
  window.setReadingMessage = function (id) {
    window.__readingId = id || ''
    var list = document.getElementById('list')
    if (!list) return
    var icons = list.querySelectorAll('.msg .actions .icon[data-action="speak"]')
    for (var i = 0; i < icons.length; i++) {
      var msg = icons[i].closest('.msg')
      var mine = msg && msg.getAttribute('data-id')
      if (id && mine === id) icons[i].classList.add('speaking')
      else icons[i].classList.remove('speaking')
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    var list = document.getElementById('list')
    installGestureContext(list)
    installMessageInteractions(list)
    list.addEventListener('scroll', function () {
      stickToBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 24
    })
    // 思考折叠：本地先切，再把状态同步给原生
    list.addEventListener('click', function (e) {
      var head = e.target.closest && e.target.closest('.reasoning-head')
      if (!head) return
      var beforeTop = head.getBoundingClientRect().top
      head.classList.toggle('open')
      var body = head.nextElementSibling
      if (body) body.classList.toggle('show')
      // 展开/收起后标题本身不该移动：被滚动锚定挪走时补回来，
      // 否则点一下标题，整段内容会跳一下（看起来像底部闪了一块颜色）
      var afterTop = head.getBoundingClientRect().top
      if (afterTop !== beforeTop) list.scrollTop += afterTop - beforeTop
      suppressStickOnce = true

      // 同步给原生：只改本地 class 的话，任何一次 setMessages 重渲染都会按旧的展开态
      // 把内容收回去（看起来就像"展开到一半被截断"）
      var msg = head.closest('.msg')
      var id = msg && msg.getAttribute('data-id')
      if (id) pendingToggleId = id
      if (id && window.AndroidHost && window.AndroidHost.messageAction) {
        window.AndroidHost.messageAction(id, 'reasoning')
      }
    })
    if (window.AndroidHost && window.AndroidHost.onReady) window.AndroidHost.onReady()
  })
})()
