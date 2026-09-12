/* ================================================================
   Django 5 实战笔记 · 共享脚本：侧边栏、目录、代码高亮、主题
   ================================================================ */
(function () {
  'use strict';

  var CHAPTERS = [
    { n: 1,  t: '创建博客应用',        d: '项目与应用、模型、迁移、Admin、ORM、视图、模板与 URL', part: '项目一 · 博客' },
    { n: 2,  t: '博客进阶：社交功能',   d: '规范 URL、分页、类视图、表单、邮件与评论系统' },
    { n: 3,  t: '扩展博客',            d: '标签、自定义模板标签、Sitemap、RSS 与全文搜索' },
    { n: 4,  t: '构建社交网站',        d: '认证框架、登录登出、注册、扩展用户资料', part: '项目二 · 图片书签社交网站' },
    { n: 5,  t: '社交登录',            d: '消息框架、自定义认证后端、Google 登录与 HTTPS 开发' },
    { n: 6,  t: '分享内容',            d: '多对多关系、书签小工具、缩略图与 fetch 异步点赞' },
    { n: 7,  t: '追踪用户行为',        d: '关注系统、活动流、信号、Debug Toolbar 与 Redis' },
    { n: 8,  t: '构建在线商店',        d: '商品目录、会话购物车、上下文处理器与 Celery', part: '项目三 · 在线商店' },
    { n: 9,  t: '支付与订单',          d: 'Stripe Checkout、Webhook、Admin 动作、CSV 与 PDF' },
    { n: 10, t: '扩展商店',            d: '优惠券系统、Stripe 折扣与 Redis 推荐引擎' },
    { n: 11, t: '国际化',              d: '翻译、Rosetta、URL 国际化、django-parler 与本地化' },
    { n: 12, t: '构建在线学习平台',     d: 'Fixtures、模型继承、自定义字段与内容模型', part: '项目四 · 在线学习平台' },
    { n: 13, t: '内容管理系统',        d: 'CBV + Mixin、权限、Formset、泛型关系与拖拽排序' },
    { n: 14, t: '渲染与缓存内容',       d: '选课、学生注册、缓存框架与 Redis 缓存' },
    { n: 15, t: '构建 API',            d: 'Django REST framework：序列化器、ViewSet、认证、权限与 API 客户端' },
    { n: 16, t: '聊天服务器',          d: 'Channels、WebSocket、异步消费者与 Redis 通道层' },
    { n: 17, t: '上线部署',            d: '多环境配置、Docker、PostgreSQL、uWSGI、Daphne、Nginx' },
    { k: 'T1', f: 't01.html', t: '测试',       d: 'TestCase 与 pytest-django、factory_boy、Mock 外部服务、覆盖率与 CI', part: '专题补充' },
    { k: 'T2', f: 't02.html', t: '项目起步',   d: 'uv、设置拆分与环境变量、自定义用户模型、ruff 与 pre-commit' },
    { k: 'T3', f: 't03.html', t: '调试与日志', d: '读懂报错、breakpoint()、shell 与 Debug Toolbar、LOGGING 与 Sentry' },
    { k: 'T4', f: 't04.html', t: 'ORM 进阶与并发', d: '聚合与 annotate、F/Q、Subquery 与 Exists、窗口函数、事务与锁' },
    { k: 'T5', f: 't05.html', t: '迁移深入',       d: '迁移原理、数据迁移、回滚与冲突、squash、不停机迁移' },
    { k: 'T6', f: 't06.html', t: '权限与安全',     d: '权限与组、对象级权限、安全总览、CSP 与安全响应头、限速' },
    { k: 'T7', f: 't07.html', t: '部署补充',       d: 'Gunicorn、WhiteNoise、对象存储、健康检查、镜像与自动部署' },
    { n: 18, k: 'A', t: '代码点评总览',        d: 'Django 5.2 最佳实践与 Two Scoops 风格的整体改进清单', part: '附录' }
  ];

  // body 的 data-chapter：章节写数字（"1"…"18"），专题写 "T1"…；首页写 "0"
  var dc = document.body.dataset.chapter || '0';
  var curIdx = -1;
  CHAPTERS.forEach(function (c, i) {
    if (String(c.n) === dc || c.k === dc) curIdx = i;
  });

  /* ---------- 侧边栏 ---------- */
  var side = document.getElementById('sidebar');
  if (side) {
    var html = '<a class="brand" href="index.html"><span class="flame">dj</span>' +
      '<span>Django 5 实战笔记<small>四个项目，从入门到上线</small></span></a>';
    CHAPTERS.forEach(function (c, i) {
      if (c.part) html += '<div class="part">' + c.part + '</div>';
      html += '<a class="ch' + (i === curIdx ? ' active' : '') + '" href="' + fileOf(c) +
        '"><span class="n">' + (c.k || c.n) + '</span><span>' + c.t + '</span></a>';
    });
    side.innerHTML = html;
    var active = side.querySelector('a.ch.active');
    if (active) setTimeout(function () {
      active.scrollIntoView({ block: 'center' });
    }, 0);
  }

  /* ---------- 移动端菜单 ---------- */
  var btn = document.createElement('button');
  btn.id = 'menu-btn';
  btn.type = 'button';
  btn.setAttribute('aria-label', '目录');
  btn.innerHTML = '&#9776;';
  btn.onclick = function () { document.body.classList.toggle('nav-open'); };
  document.body.appendChild(btn);
  document.addEventListener('click', function (e) {
    if (document.body.classList.contains('nav-open') &&
        side && !side.contains(e.target) && e.target !== btn) {
      document.body.classList.remove('nav-open');
    }
  });

  /* ---------- 主题切换 ---------- */
  var tbtn = document.createElement('button');
  tbtn.id = 'theme-btn';
  tbtn.type = 'button';
  tbtn.setAttribute('aria-label', '切换深浅色');
  tbtn.innerHTML = '&#9789;';
  tbtn.onclick = function () {
    var root = document.documentElement;
    var now = root.getAttribute('data-theme');
    var dark = now ? now === 'dark'
      : window.matchMedia('(prefers-color-scheme: dark)').matches;
    root.setAttribute('data-theme', dark ? 'light' : 'dark');
    try { localStorage.setItem('dj5-doc-theme', dark ? 'light' : 'dark'); } catch (e) {}
  };
  document.body.appendChild(tbtn);
  try {
    var saved = localStorage.getItem('dj5-doc-theme');
    if (saved) document.documentElement.setAttribute('data-theme', saved);
  } catch (e) {}

  /* ---------- 箭头 marker（全局一次） ---------- */
  var defs = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  defs.setAttribute('width', '0'); defs.setAttribute('height', '0');
  defs.setAttribute('style', 'position:absolute');
  // SVG marker 的内容不会从引用它的元素继承 color，所以直接用 CSS 变量填色。
  defs.innerHTML =
    '<defs>' +
    '<marker id="ar" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">' +
    '<path d="M0,0 L10,5 L0,10 z" fill="var(--fg-faint)"/></marker>' +
    '<marker id="ar-a" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">' +
    '<path d="M0,0 L10,5 L0,10 z" fill="var(--accent)"/></marker>' +
    '<marker id="ar-b" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">' +
    '<path d="M0,0 L10,5 L0,10 z" fill="var(--blue)"/></marker>' +
    '<marker id="ar-g" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">' +
    '<path d="M0,0 L10,5 L0,10 z" fill="var(--green)"/></marker>' +
    '<marker id="ar-r" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">' +
    '<path d="M0,0 L10,5 L0,10 z" fill="var(--red)"/></marker>' +
    '<marker id="ar-p" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">' +
    '<path d="M0,0 L10,5 L0,10 z" fill="var(--purple)"/></marker>' +
    '</defs>';
  document.body.appendChild(defs);

  /* ---------- 章节内目录 ---------- */
  var main = document.querySelector('main');
  var slot = document.getElementById('chapter-toc');
  if (slot && main) {
    var hs = main.querySelectorAll('h2');
    if (hs.length > 2) {
      var t = '<div class="h">本章目录</div><ol>';
      hs.forEach(function (h, i) {
        if (!h.id) h.id = 'sec-' + (i + 1);
        t += '<li><a href="#' + h.id + '">' + h.textContent + '</a></li>';
      });
      slot.className = 'toc';
      slot.innerHTML = t + '</ol>';
    }
  }

  /* ---------- 代码高亮 ---------- */
  var PY_KW = ('def|class|return|if|elif|else|for|while|in|not|and|or|is|None|True|False|' +
    'import|from|as|with|try|except|finally|raise|pass|lambda|yield|async|await|' +
    'global|nonlocal|del|assert|break|continue|self|super|cls|match|case').split('|');

  var RE_PY = new RegExp(
    '(#[^\\n]*)' +                                                          // 1 注释
    '|([rRfFbBuU]{0,2}"""[\\s\\S]*?"""|[rRfFbBuU]{0,2}\'\'\'[\\s\\S]*?\'\'\'' +
      '|[rRfFbBuU]{0,2}"(?:\\\\.|[^"\\\\\\n])*"|[rRfFbBuU]{0,2}\'(?:\\\\.|[^\'\\\\\\n])*\')' + // 2 字符串
    '|(@[A-Za-z_][\\w.]*)' +                                                // 3 装饰器
    '|\\b(' + PY_KW.join('|') + ')\\b' +                                    // 4 关键字
    '|\\b([A-Z][A-Za-z0-9_]*)\\b' +                                         // 5 类名
    '|\\b(\\d+(?:\\.\\d+)?)\\b' +                                           // 6 数字
    '|\\b([a-zA-Z_][\\w]*)(?=\\()',                                         // 7 函数调用
    'g');

  var JS_KW = ('const|let|var|function|return|if|else|await|async|import|from|export|default|new|class|' +
    'extends|for|while|of|in|do|try|catch|finally|throw|switch|case|break|continue|' +
    'typeof|instanceof|delete|void|null|undefined|true|false|this|super|static').split('|');

  var RE_JS = new RegExp(
    '(\\/\\*[\\s\\S]*?\\*\\/|\\/\\/[^\\n]*)' +
    '|(`(?:\\\\[\\s\\S]|[^\\\\`])*`|\'(?:\\\\[\\s\\S]|[^\\\\\'])*\'|"(?:\\\\[\\s\\S]|[^\\\\"])*")' +
    '|\\b(' + JS_KW.join('|') + ')\\b' +
    '|\\b([A-Z][A-Za-z0-9_]*)\\b' +
    '|\\b(\\d+(?:\\.\\d+)?)\\b' +
    '|\\b([a-zA-Z_$][\\w$]*)(?=\\()',
    'g');

  // Django 模板 / HTML
  var RE_TPL = /(\{#[\s\S]*?#\}|<!--[\s\S]*?-->)|(\{%[\s\S]*?%\})|(\{\{[\s\S]*?\}\})|(<\/?[a-zA-Z][\w-]*|\/?>)|("[^"\n]*")/g;

  var RE_SH = /(#[^\n]*)|('(?:\\[\s\S]|[^\\'])*'|"(?:\\[\s\S]|[^\\"])*")|\b(python3?|py|pip|django-admin|manage\.py|docker|compose|celery|redis-server|redis-cli|git|cd|mkdir|curl|source|export|stripe|brew|sudo|apt|apt-get|uv|openssl|mkcert|gunicorn|uwsgi|daphne|FROM|RUN|COPY|ENV|WORKDIR|CMD|EXPOSE)\b/g;

  var RE_SQL = /(--[^\n]*)|('(?:[^'])*')|\b(SELECT|FROM|WHERE|AND|OR|NOT|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|INDEX|ON|JOIN|INNER|LEFT|ORDER|BY|GROUP|HAVING|LIMIT|OFFSET|AS|DESC|ASC|COUNT|BEGIN|COMMIT|PRIMARY|KEY|REFERENCES|NULL|EXTENSION|IF|EXISTS|LIKE|DISTINCT|integer|varchar|text|datetime|bigint|boolean)\b/g;

  function esc(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function paint(src, re, classes) {
    var out = '', last = 0, m;
    re.lastIndex = 0;
    while ((m = re.exec(src)) !== null) {
      out += esc(src.slice(last, m.index));
      for (var g = 1; g < m.length; g++) {
        if (m[g] !== undefined) { out += '<span class="' + classes[g - 1] + '">' + esc(m[g]) + '</span>'; break; }
      }
      last = m.index + m[0].length;
      if (m[0].length === 0) re.lastIndex++;
    }
    return out + esc(src.slice(last));
  }

  var LANG_LABEL = { py: 'python', python: 'python', html: 'django 模板', django: 'django 模板',
    bash: 'shell', sh: 'shell', shell: 'shell', sql: 'sql', js: 'javascript', javascript: 'javascript',
    text: 'text', yaml: 'yaml', ini: 'ini', dockerfile: 'dockerfile', nginx: 'nginx', env: '.env', json: 'json', css: 'css' };

  document.querySelectorAll('.code').forEach(function (box) {
    var pre = box.querySelector('pre');
    if (!pre) return;
    var lang = box.dataset.lang || '';
    var file = box.dataset.file || '';
    var bar = document.createElement('div');
    bar.className = 'bar';
    bar.innerHTML = '<span class="tag">' + esc(file || LANG_LABEL[lang] || lang || 'code') + '</span>';
    var cp = document.createElement('button');
    cp.className = 'copy'; cp.type = 'button'; cp.textContent = '复制';
    cp.onclick = function () {
      var txt = pre.textContent;
      if (navigator.clipboard) navigator.clipboard.writeText(txt);
      cp.textContent = '已复制'; setTimeout(function () { cp.textContent = '复制'; }, 1400);
    };
    bar.appendChild(cp);
    box.insertBefore(bar, pre);

    var code = pre.textContent.replace(/^\n/, '').replace(/\s+$/, '');
    if (lang === 'py' || lang === 'python') {
      pre.innerHTML = paint(code, RE_PY, ['tk-cm', 'tk-st', 'tk-fn', 'tk-kw', 'tk-tp', 'tk-nm', 'tk-fn']);
    } else if (lang === 'html' || lang === 'django') {
      pre.innerHTML = paint(code, RE_TPL, ['tk-cm', 'tk-kw', 'tk-nm', 'tk-fn', 'tk-st']);
    } else if (lang === 'js' || lang === 'javascript' || lang === 'json') {
      pre.innerHTML = paint(code, RE_JS, ['tk-cm', 'tk-st', 'tk-kw', 'tk-tp', 'tk-nm', 'tk-fn']);
    } else if (lang === 'sql') {
      pre.innerHTML = paint(code, RE_SQL, ['tk-cm', 'tk-st', 'tk-kw']);
    } else {
      pre.innerHTML = paint(code, RE_SH, ['tk-cm', 'tk-st', 'tk-kw']);
    }
  });

  /* ---------- 上一章 / 下一章 ---------- */
  var pager = document.getElementById('pager');
  if (pager && curIdx >= 0) {
    var prev = CHAPTERS[curIdx - 1];
    var next = CHAPTERS[curIdx + 1];
    var h = '';
    h += prev ? '<a class="prev" href="' + fileOf(prev) + '"><span>&larr; 上一篇</span>' + label(prev) + '</a>'
              : '<a class="prev" href="index.html"><span>&larr; 返回</span>课程首页</a>';
    h += next ? '<a class="next" href="' + fileOf(next) + '"><span>下一篇 &rarr;</span>' + label(next) + '</a>'
              : '<a class="next" href="index.html"><span>完结 &rarr;</span>回到课程首页</a>';
    pager.className = 'pager';
    pager.innerHTML = h;
  }

  /* ---------- 首页目录 ---------- */
  var grid = document.getElementById('toc-grid');
  if (grid) {
    var g = '';
    CHAPTERS.forEach(function (c) {
      g += '<a class="toc-card" href="' + fileOf(c) + '">' +
        '<div class="n">' + kicker(c) + '</div>' +
        '<div class="t">' + c.t + '</div>' +
        '<div class="d">' + c.d + '</div></a>';
    });
    grid.className = 'toc-grid';
    grid.innerHTML = g;
  }

  function fileOf(c) { return c.f || ('ch' + pad(c.n) + '.html'); }
  function kicker(c) { return c.k === 'A' ? '附录' : c.k ? '专题 ' + c.k.slice(1) : '第 ' + c.n + ' 章'; }
  function label(c) { return kicker(c) + ' · ' + c.t; }
  function pad(n) { return n < 10 ? '0' + n : '' + n; }
})();
