const fs = require('fs');
const path = require('path');
const { pathToFileURL } = require('url');

const option = (name) => {
  const index = process.argv.indexOf(name);
  return index < 0 ? null : process.argv[index + 1];
};
const root = path.resolve(option('--book-root') || __dirname);
const frontend = path.resolve(option('--frontend-root') || path.join(root, '..', '..', 'frontend'));
const manuscript = path.join(root, 'manuscrito');
const dist = path.join(root, 'dist');
const htmlFile = path.join(dist, 'financial-control-livro.html');
const pdfFile = path.join(dist, 'financial-control-livro.pdf');
const reportFile = path.join(dist, 'validation-report.json');
const inspection = path.join(dist, 'inspection');

function loadPlaywright() {
  for (const modulePath of [
    path.join(frontend, 'node_modules', 'playwright'),
    path.join(frontend, 'node_modules', '@playwright', 'test'),
  ]) {
    try {
      return require(modulePath);
    } catch {
      // Only use the frontend dependency already present.
    }
  }
  throw new Error(`Playwright não encontrado em ${path.join(frontend, 'node_modules')}.`);
}

const escapeHtml = (text) =>
  text
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');

function inline(text) {
  const snippets = [];
  let value = text.replace(/`([^`]+)`/g, (_, code) => {
    const marker = `\u0000${snippets.length}\u0000`;
    snippets.push(`<code>${escapeHtml(code)}</code>`);
    return marker;
  });
  value = escapeHtml(value)
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>');
  return value.replace(/\u0000(\d+)\u0000/g, (_, index) => snippets[Number(index)]);
}

function markdownToHtml(text, file, registry) {
  const lines = text.replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n').split('\n');
  const blocks = [];
  const isSeparator = (line) =>
    line.includes('-') && /^\s*\|?[\s:|-]+\|[\s:|-]*\|?\s*$/.test(line);
  const tableCells = (line) =>
    line
      .trim()
      .replace(/^\|/, '')
      .replace(/\|$/, '')
      .split('|')
      .map((cell) => inline(cell.trim()));
  const isBlock = (index) => {
    const line = lines[index] || '';
    return (
      !line.trim() ||
      /^#{1,4}\s+/.test(line) ||
      /^```/.test(line) ||
      /^>\s?/.test(line) ||
      /^\s*[-*+]\s+/.test(line) ||
      /^\s*\d+\.\s+/.test(line) ||
      (line.includes('|') && isSeparator(lines[index + 1] || ''))
    );
  };
  let lineNumber = 0;
  while (lineNumber < lines.length) {
    const line = lines[lineNumber];
    if (!line.trim()) {
      lineNumber += 1;
      continue;
    }
    const heading = /^(#{1,4})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      const title = heading[2].trim();
      const key = `heading-${registry.next++}`;
      const part = level === 1 && /^PARTE\b/i.test(title) ? ' class="part-title"' : '';
      blocks.push(`<h${level}${part} data-heading-key="${key}">${inline(title)}</h${level}>`);
      registry.headings.push({ key, level, title, file });
      lineNumber += 1;
      continue;
    }
    if (/^```/.test(line)) {
      const language = line.slice(3).trim() || 'text';
      const code = [];
      for (
        lineNumber += 1;
        lineNumber < lines.length && !/^```/.test(lines[lineNumber]);
        lineNumber += 1
      ) {
        code.push(lines[lineNumber]);
      }
      if (lineNumber < lines.length) lineNumber += 1;
      blocks.push(
        `<pre data-language="${escapeHtml(language)}"><code>${escapeHtml(code.join('\n'))}</code></pre>`,
      );
      continue;
    }
    if (line.includes('|') && isSeparator(lines[lineNumber + 1] || '')) {
      const header = tableCells(line);
      const rows = [];
      lineNumber += 2;
      while (
        lineNumber < lines.length &&
        lines[lineNumber].trim() &&
        lines[lineNumber].includes('|')
      ) {
        rows.push(tableCells(lines[lineNumber]));
        lineNumber += 1;
      }
      blocks.push(
        `<table><thead><tr>${header.map((cell) => `<th>${cell}</th>`).join('')}</tr></thead>` +
          `<tbody>${rows
            .map(
              (row) =>
                `<tr>${header.map((_, index) => `<td>${row[index] || ''}</td>`).join('')}</tr>`,
            )
            .join('')}</tbody></table>`,
      );
      continue;
    }
    if (/^>\s?/.test(line)) {
      const quote = [];
      while (lineNumber < lines.length && /^>\s?/.test(lines[lineNumber])) {
        quote.push(lines[lineNumber].replace(/^>\s?/, ''));
        lineNumber += 1;
      }
      blocks.push(`<blockquote><p>${inline(quote.join(' '))}</p></blockquote>`);
      continue;
    }
    const unordered = /^\s*[-*+]\s+/.test(line);
    const ordered = /^\s*\d+\.\s+/.test(line);
    if (unordered || ordered) {
      const pattern = unordered ? /^\s*[-*+]\s+(.+)$/ : /^\s*\d+\.\s+(.+)$/;
      const items = [];
      while (lineNumber < lines.length) {
        const match = pattern.exec(lines[lineNumber]);
        if (!match) break;
        items.push(`<li>${inline(match[1])}</li>`);
        lineNumber += 1;
      }
      const tag = unordered ? 'ul' : 'ol';
      blocks.push(`<${tag}>${items.join('')}</${tag}>`);
      continue;
    }
    const paragraph = [line.trim()];
    for (lineNumber += 1; lineNumber < lines.length && !isBlock(lineNumber); lineNumber += 1) {
      paragraph.push(lines[lineNumber].trim());
    }
    blocks.push(`<p>${inline(paragraph.join(' '))}</p>`);
  }
  return `<section class="chapter-source" data-file="${escapeHtml(file)}">${blocks.join(
    '\n',
  )}</section>`;
}

const glossary = [
  ['API', 'Fronteira HTTP REST exposta em /api/v1.'],
  ['Controller', 'Camada que traduz HTTP e delega ao Service.'],
  ['DTO', 'Contrato de entrada ou saída separado da Entity.'],
  ['Entity', 'Classe JPA mapeada para uma tabela.'],
  ['Flyway', 'Ferramenta de migrations versionadas.'],
  ['Hibernate', 'ORM usado pelo Spring Data JPA.'],
  ['JWT', 'Access token HS256 usado na autenticação stateless.'],
  ['Migration', 'Alteração versionada e imutável do schema.'],
  ['Ownership', 'Isolamento por usuário na aplicação e no banco.'],
  ['PostgreSQL', 'Banco relacional oficial do projeto.'],
  ['Repository', 'Interface Spring Data de persistência.'],
  ['Service', 'Camada de regras e transações.'],
  ['Testcontainers', 'PostgreSQL real executado em testes.'],
  ['UUID v7', 'Identificador temporal gerado pela aplicação.'],
];

function paginationScript() {
  return String.raw`
(() => {
  const source = document.querySelector('#source');
  const staging = document.querySelector('#staging');
  const book = document.querySelector('#book');
  const headings = JSON.parse(document.querySelector('#heading-data').textContent);
  const glossary = ${JSON.stringify(glossary)};
  const make = (html) => {
    const template = document.createElement('template');
    template.innerHTML = html.trim();
    return template.content.firstElementChild;
  };
  const createPage = (kind, header, label) => {
    const page = make('<section class="page ' + kind + '"><div class="running-header"></div>' +
      '<main class="page-body"></main><footer class="page-footer"></footer></section>');
    page.dataset.label = label || '';
    page.querySelector('.running-header').textContent = header || '';
    page.querySelector('.page-footer').textContent = label || '';
    staging.appendChild(page);
    return page;
  };
  const overflows = (body) =>
    body.scrollHeight > body.clientHeight + 1 || body.scrollWidth > body.clientWidth + 1;

  function append(block, state) {
    const clone = block.cloneNode(true);
    state.body.appendChild(clone);
    if (!overflows(state.body)) return;
    clone.remove();
    state.next();
    state.body.appendChild(clone);
    if (!overflows(state.body)) return;
    clone.remove();
    if (block.tagName === 'TABLE') {
      const head = block.querySelector('thead');
      let table;
      let tbody;
      const reset = () => {
        table = block.cloneNode(false);
        if (head) table.appendChild(head.cloneNode(true));
        tbody = document.createElement('tbody');
        table.appendChild(tbody);
        state.body.appendChild(table);
      };
      reset();
      block.querySelectorAll('tbody tr').forEach((row) => {
        tbody.appendChild(row.cloneNode(true));
        if (overflows(state.body)) {
          tbody.lastElementChild.remove();
          state.next();
          reset();
          tbody.appendChild(row.cloneNode(true));
        }
      });
    } else if (block.tagName === 'UL' || block.tagName === 'OL') {
      let list = block.cloneNode(false);
      state.body.appendChild(list);
      [...block.children].forEach((item) => {
        list.appendChild(item.cloneNode(true));
        if (overflows(state.body)) {
          list.lastElementChild.remove();
          state.next();
          list = block.cloneNode(false);
          state.body.appendChild(list);
          list.appendChild(item.cloneNode(true));
        }
      });
    } else if (block.tagName === 'PRE') {
      let pre = block.cloneNode(false);
      let code = document.createElement('code');
      pre.appendChild(code);
      state.body.appendChild(pre);
      block.textContent.split('\n').forEach((line) => {
        const previous = code.textContent;
        code.textContent = previous ? previous + '\n' + line : line;
        if (overflows(state.body)) {
          code.textContent = previous;
          state.next();
          pre = block.cloneNode(false);
          code = document.createElement('code');
          code.textContent = line;
          pre.appendChild(code);
          state.body.appendChild(pre);
        }
      });
    } else {
      state.body.appendChild(clone);
      clone.classList.add('oversize-block');
    }
  }

  const contentPages = [];
  let contentNumber = 0;
  let header = 'Financial Control';
  let current;
  const state = {
    body: null,
    next() {
      contentNumber += 1;
      current = createPage('content', header, String(contentNumber));
      contentPages.push(current);
      state.body = current.querySelector('.page-body');
    },
  };
  source.querySelectorAll('.chapter-source').forEach((chapter) => {
    const blocks = [...chapter.children];
    const title = blocks.find((block) =>
      block.tagName === 'H1' && !block.classList.contains('part-title'));
    header = title ? title.textContent : chapter.dataset.file;
    if (!current || state.body.children.length) state.next();
    blocks.forEach((block, index) => {
      const chapterTitle = block.tagName === 'H1' && !block.classList.contains('part-title');
      if (chapterTitle && state.body.children.length &&
          ![...state.body.children].every((child) => child.classList.contains('part-title'))) {
        state.next();
      }
      if (/^H[2-4]$/.test(block.tagName) && blocks[index + 1]) {
        const heading = block.cloneNode(true);
        const following = blocks[index + 1].cloneNode(true);
        state.body.append(heading, following);
        const orphan = overflows(state.body);
        heading.remove();
        following.remove();
        if (orphan && state.body.children.length) state.next();
      }
      append(block, state);
    });
  });
  const headingPages = {};
  contentPages.forEach((page) => page.querySelectorAll('[data-heading-key]').forEach((heading) => {
    headingPages[heading.dataset.headingKey] = page.dataset.label;
  }));

  function paginate(nodes, kind, pageHeader, first, label = String) {
    const pages = [];
    let number = first;
    let page = createPage(kind, pageHeader, label(number));
    pages.push(page);
    const local = {
      body: page.querySelector('.page-body'),
      next() {
        number += 1;
        page = createPage(kind, pageHeader, label(number));
        pages.push(page);
        local.body = page.querySelector('.page-body');
      },
    };
    nodes.forEach((node) => append(node, local));
    return pages;
  }
  const glossaryNodes = [
    make('<h1>Glossário</h1>'),
    make('<p class="book-note">Termos contextualizados ao conteúdo deste volume.</p>'),
    ...glossary.map(([term, definition]) =>
      make('<dl class="glossary-entry"><dt>' + term + '</dt><dd>' + definition + '</dd></dl>')),
  ];
  const glossaryPages = paginate(
    glossaryNodes, 'appendix glossary', 'Glossário', contentNumber + 1);

  const entries = [];
  [...glossary].sort((a, b) => a[0].localeCompare(b[0], 'pt-BR')).forEach(([term]) => {
    const needle = term.toLocaleLowerCase('pt-BR');
    const pages = contentPages
      .filter((page) => page.innerText.toLocaleLowerCase('pt-BR').includes(needle))
      .map((page) => page.dataset.label);
    if (pages.length) entries.push(make(
      '<div class="index-entry"><span>' + term + '</span><span class="index-pages">' +
      pages.join(', ') + '</span></div>'));
  });
  const grid = make('<div class="index-grid"></div>');
  entries.forEach((entry) => grid.appendChild(entry));
  const indexStart = contentNumber + glossaryPages.length + 1;
  const indexPages = paginate([
    make('<h1>Índice remissivo</h1>'),
    make('<p class="book-note">Referências calculadas pelas ocorrências nas páginas finais; não são estimativas.</p>'),
    grid,
  ], 'appendix index', 'Índice remissivo', indexStart);

  const roman = (number) => {
    const values = [[10,'x'],[9,'ix'],[5,'v'],[4,'iv'],[1,'i']];
    let result = '';
    values.forEach(([value, symbol]) => {
      while (number >= value) { result += symbol; number -= value; }
    });
    return result;
  };
  const toc = [make('<h1 class="toc-title">Sumário</h1>')];
  headings
    .filter((heading) => heading.level <= 2 && !/^PARTE\b/i.test(heading.title))
    .forEach((heading) => {
      if (!headingPages[heading.key]) return;
      toc.push(make('<div class="toc-row toc-level-' + heading.level + '">' +
        '<span class="toc-label">' + heading.title + '</span>' +
        '<span class="toc-page">' + headingPages[heading.key] + '</span></div>'));
    });
  toc.push(make('<div class="toc-row toc-level-1"><span class="toc-label">Glossário</span>' +
    '<span class="toc-page">' + (contentNumber + 1) + '</span></div>'));
  toc.push(make('<div class="toc-row toc-level-1"><span class="toc-label">Índice remissivo</span>' +
    '<span class="toc-page">' + indexStart + '</span></div>'));
  const tocPages = paginate(toc, 'frontmatter toc', 'Sumário', 1, roman);

  const cover = make('<section class="page cover"><div class="cover-content">' +
    '<p class="cover-kicker">Livro técnico</p><h1>Financial Control</h1>' +
    '<p class="cover-subtitle">Arquitetura, persistência e desenvolvimento do sistema de controle financeiro pessoal</p>' +
    '</div><div class="cover-meta"><span>Felipe</span><span>Edição auditada — agosto de 2026</span></div></section>');
  const all = [cover, ...tocPages, ...contentPages, ...glossaryPages, ...indexPages];
  book.replaceChildren(...all);
  all.forEach((page, index) => {
    page.classList.add((index + 1) % 2 ? 'odd' : 'even');
    page.dataset.physicalPage = String(index + 1);
  });
  source.remove();
  staging.remove();
  const emptyPages = all.filter((page) => !page.classList.contains('cover'))
    .filter((page) => !page.querySelector('.page-body')?.innerText.trim())
    .map((page) => page.dataset.physicalPage);
  const overflowDetails = all.map((page) => {
    const body = page.querySelector('.page-body');
    return body ? {
      page: page.dataset.physicalPage,
      heightDelta: body.scrollHeight - body.clientHeight,
      widthDelta: body.scrollWidth - body.clientWidth,
    } : null;
  }).filter((detail) => detail && (detail.heightDelta > 1 || detail.widthDelta > 1));
  const overflowPages = overflowDetails.map((detail) => detail.page);
  const outOfBoundsElements = [];
  all.forEach((page) => {
    const body = page.querySelector('.page-body');
    if (!body) return;
    const bodyRect = body.getBoundingClientRect();
    page.querySelectorAll('pre, table').forEach((element) => {
      const rect = element.getBoundingClientRect();
      if (
        rect.left < bodyRect.left - 1 ||
        rect.right > bodyRect.right + 1 ||
        rect.top < bodyRect.top - 1 ||
        rect.bottom > bodyRect.bottom + 1
      ) {
        outOfBoundsElements.push({
          page: page.dataset.physicalPage,
          tag: element.tagName,
          text: element.innerText.slice(0, 80),
        });
      }
    });
  });
  const orphanHeadings = all.flatMap((page) => {
    const body = page.querySelector('.page-body');
    if (!body || !body.lastElementChild || !/^H[2-4]$/.test(body.lastElementChild.tagName)) {
      return [];
    }
    return [{
      page: page.dataset.physicalPage,
      heading: body.lastElementChild.innerText,
    }];
  });
  const pageSummaries = all.map((page) => {
    const body = page.querySelector('.page-body');
    const text = body?.innerText.trim() || page.innerText.trim();
    if (!body) {
      return { page: page.dataset.physicalPage, kind: 'cover', characters: text.length };
    }
    const children = [...body.children];
    const bottom = children.length
      ? Math.max(...children.map((child) => child.getBoundingClientRect().bottom))
      : body.getBoundingClientRect().top;
    const bodyRect = body.getBoundingClientRect();
    return {
      page: page.dataset.physicalPage,
      kind: page.classList.contains('toc')
        ? 'toc'
        : page.classList.contains('glossary')
          ? 'glossary'
          : page.classList.contains('index')
            ? 'index'
            : 'content',
      label: page.dataset.label,
      characters: text.length,
      fillPercent: Math.round(((bottom - bodyRect.top) / bodyRect.height) * 100),
      firstText: text.slice(0, 90),
      lastText: text.slice(-90),
    };
  });
  const representativeBody = contentPages[0].querySelector('.page-body');
  const representativeStyle = getComputedStyle(representativeBody);
  window.__BOOK_REPORT__ = {
    physicalPages: all.length,
    contentPages: contentPages.length,
    tocPages: tocPages.length,
    glossaryPages: glossaryPages.length,
    indexPages: indexPages.length,
    emptyPages,
    overflowPages,
    overflowDetails,
    outOfBoundsElements,
    orphanHeadings,
    pageSummaries,
    typography: {
      fontSize: representativeStyle.fontSize,
      lineHeight: representativeStyle.lineHeight,
      pageWidth: getComputedStyle(contentPages[0]).width,
      pageHeight: getComputedStyle(contentPages[0]).height,
    },
    headings: headings.length,
    tables: book.querySelectorAll('table').length,
    codeBlocks: book.querySelectorAll('pre').length,
    diagrams: [...book.querySelectorAll('pre')].filter((pre) => /[↓→├└│]/.test(pre.textContent)).length,
    indexEntries: entries.length,
    portugueseCharacters: /[áàâãéêíóôõúç]/i.test(book.innerText),
  };
  window.__BOOK_READY__ = true;
})();
`;
}

async function main() {
  const files = fs
    .readdirSync(manuscript, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.toLowerCase().endsWith('.md'))
    .map((entry) => entry.name)
    .sort((a, b) => a.localeCompare(b, 'pt-BR', { numeric: true }));
  if (!files.length) throw new Error(`Nenhum capítulo encontrado em ${manuscript}.`);
  fs.mkdirSync(dist, { recursive: true });
  fs.mkdirSync(inspection, { recursive: true });
  const registry = { headings: [], next: 1 };
  const chapters = files.map((file) =>
    markdownToHtml(fs.readFileSync(path.join(manuscript, file), 'utf8'), file, registry));
  const css = fs.readFileSync(path.join(root, 'assets', 'book.css'), 'utf8');
  const html = `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Financial Control — Livro técnico</title><style>${css}</style></head><body>
<div id="book"></div><div id="source" aria-hidden="true">${chapters.join('\n')}</div>
<div id="staging" aria-hidden="true"></div>
<script id="heading-data" type="application/json">${JSON.stringify(registry.headings).replaceAll(
    '<',
    '\\u003c',
  )}</script><script>${paginationScript()}</script></body></html>`;
  fs.writeFileSync(htmlFile, html, 'utf8');

  const { chromium } = loadPlaywright();
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  await page.goto(pathToFileURL(htmlFile).href, { waitUntil: 'load' });
  await page.waitForFunction(() => window.__BOOK_READY__ === true, null, { timeout: 120000 });
  const report = await page.evaluate(() => window.__BOOK_REPORT__);
  if (report.emptyPages.length || report.overflowPages.length) {
    await browser.close();
    throw new Error(
      `Paginação inválida. Vazias: ${report.emptyPages.join(',') || 'nenhuma'}; ` +
        `overflow: ${JSON.stringify(report.overflowDetails)}`,
    );
  }
  await page.pdf({
    path: pdfFile,
    width: '210mm',
    height: '297mm',
    margin: { top: '0', right: '0', bottom: '0', left: '0' },
    preferCSSPageSize: true,
    printBackground: true,
    displayHeaderFooter: false,
    tagged: true,
    outline: true,
  });
  const pdfProbe = await browser.newPage({ viewport: { width: 1000, height: 760 } });
  await pdfProbe.setContent(
    `<embed src="${pathToFileURL(pdfFile).href}" type="application/pdf" ` +
      'style="position:fixed;inset:0;width:100%;height:100%">',
    { waitUntil: 'load' },
  );
  await pdfProbe.waitForTimeout(750);
  const pdfEmbedState = await pdfProbe.locator('embed').evaluate((embed) => ({
    type: embed.type,
    width: embed.getBoundingClientRect().width,
    height: embed.getBoundingClientRect().height,
  }));
  await pdfProbe.close();
  const pages = page.locator('#book > .page');
  const count = await pages.count();
  const samples = [
    ['inicio', 0],
    ['meio', Math.floor((count - 1) / 2)],
    ['final', count - 1],
  ];
  for (const [name, index] of samples) {
    await pages.nth(index).screenshot({
      path: path.join(inspection, `${name}-pagina-${index + 1}.png`),
    });
  }
  await browser.close();

  const pdf = fs.readFileSync(pdfFile);
  const pdfLatin = pdf.toString('latin1');
  const validation = {
    generatedAt: new Date().toISOString(),
    chapters: files,
    chapterCount: files.length,
    htmlPath: htmlFile,
    htmlBytes: fs.statSync(htmlFile).size,
    pdfPath: pdfFile,
    pdfBytes: fs.statSync(pdfFile).size,
    pdfHeader: pdf.subarray(0, 5).toString('ascii') === '%PDF-',
    pdfEof: pdfLatin.includes('%%EOF'),
    pdfPageObjects: [...pdfLatin.matchAll(/\/Type\s*\/Page\b/g)].length,
    pdfEmbedState,
    ...report,
    inspectionImages: samples.map(([name, index]) =>
      path.join(inspection, `${name}-pagina-${index + 1}.png`)),
  };
  if (!validation.pdfHeader || !validation.pdfEof || validation.pdfBytes < 10000) {
    throw new Error('PDF criado, mas reprovado na validação estrutural.');
  }
  if (
    validation.pdfEmbedState.type !== 'application/pdf' ||
    validation.pdfEmbedState.width < 900 ||
    validation.pdfEmbedState.height < 700
  ) {
    throw new Error('O PDF não foi incorporado corretamente no Chromium.');
  }
  if (validation.pdfPageObjects !== validation.physicalPages) {
    throw new Error(
      `O PDF contém ${validation.pdfPageObjects} páginas, mas o HTML paginado contém ` +
        `${validation.physicalPages}.`,
    );
  }
  if (validation.outOfBoundsElements.length || validation.orphanHeadings.length) {
    throw new Error('A inspeção de layout encontrou corte de conteúdo ou título órfão.');
  }
  fs.writeFileSync(reportFile, JSON.stringify(validation, null, 2), 'utf8');
  console.log(`Capítulos: ${files.length}`);
  console.log(`Páginas físicas: ${report.physicalPages}`);
  console.log(`HTML: ${htmlFile}`);
  console.log(`PDF: ${pdfFile}`);
  console.log(`Relatório: ${reportFile}`);
}

main().catch((error) => {
  console.error(error.stack || error.message || String(error));
  process.exitCode = 1;
});

