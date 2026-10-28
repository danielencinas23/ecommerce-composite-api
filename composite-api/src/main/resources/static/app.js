// Cliente web: hace UNA sola petición a la Composite API y pinta el resultado.
const form = document.getElementById('search');
const input = document.getElementById('productId');
const result = document.getElementById('result');
const jsonOut = document.getElementById('json');
const endpointOut = document.getElementById('endpoint');
const timingOut = document.getElementById('timing');

const usd = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'USD' });
const mxn = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' });

const STOCK_LABELS = {
  IN_STOCK: ['En stock', 'ok'],
  LOW_STOCK: ['Stock bajo', 'warn'],
  OUT_OF_STOCK: ['Agotado', 'bad'],
  UNKNOWN: ['No disponible', 'neutral'],
};
const STATUS_LABELS = {
  SUCCESS: ['SUCCESS', 'ok'],
  PARTIAL_SUCCESS: ['PARTIAL_SUCCESS', 'warn'],
  NOT_FOUND: ['NOT_FOUND', 'bad'],
  ERROR: ['ERROR', 'bad'],
};

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
}

function badge([label, tone]) {
  return `<span class="badge ${tone}">${escapeHtml(label)}</span>`;
}

function errorList(errors = []) {
  return errors.map((e) => `<li><strong>${escapeHtml(e.service)}:</strong> ${escapeHtml(e.message)}</li>`).join('');
}

function renderSummary(data) {
  const stock = STOCK_LABELS[data.stock_status] ?? STOCK_LABELS.UNKNOWN;
  const priceMxn = data.price_mxn != null ? mxn.format(data.price_mxn) : '—';
  const rateHint = data.exchange_rate_usd_mxn != null ? `1 USD = ${data.exchange_rate_usd_mxn} MXN` : 'Tipo de cambio no disponible';
  const units = data.available_units != null ? `${data.available_units} unidades` : '—';
  const delivery = data.estimated_delivery_days != null ? `${data.estimated_delivery_days} días` : '—';

  result.innerHTML = `
    <article class="card">
      <div class="card-head">
        <div>
          <div class="meta">${escapeHtml(data.product_id)} · ${escapeHtml(data.category)}</div>
          <h2>${escapeHtml(data.title)}</h2>
        </div>
        ${badge(STATUS_LABELS[data.status] ?? STATUS_LABELS.ERROR)}
      </div>
      <p class="desc">${escapeHtml(data.description)}</p>

      <div class="grid">
        <div class="stat">
          <div class="label">Precio base</div>
          <div class="value">${usd.format(data.price_usd)}</div>
          <div class="hint">Catálogo</div>
        </div>
        <div class="stat">
          <div class="label">Precio en México</div>
          <div class="value">${priceMxn}</div>
          <div class="hint">${escapeHtml(rateHint)}</div>
        </div>
        <div class="stat">
          <div class="label">Disponibilidad</div>
          <div class="value">${badge(stock)}</div>
          <div class="hint">${escapeHtml(units)}</div>
        </div>
        <div class="stat">
          <div class="label">Entrega estimada</div>
          <div class="value">${escapeHtml(delivery)}</div>
          <div class="hint">${escapeHtml(data.warehouse ?? 'Almacén no disponible')}</div>
        </div>
      </div>

      <p class="meta">Fuente del tipo de cambio: ${escapeHtml(data.currency_conversion_source)}</p>

      ${data.errors ? `
        <div class="alerts">
          Algunos servicios no respondieron; se muestran los datos disponibles.
          <ul>${errorList(data.errors)}</ul>
        </div>` : ''}
    </article>`;
}

function renderFailure(data) {
  result.innerHTML = `
    <article class="card">
      <div class="card-head">
        <h2>${escapeHtml(data.product_id ?? 'Error')}</h2>
        ${badge(STATUS_LABELS[data.status] ?? STATUS_LABELS.ERROR)}
      </div>
      <div class="alerts bad">
        ${escapeHtml(data.message ?? 'No fue posible obtener el producto.')}
        ${data.errors ? `<ul>${errorList(data.errors)}</ul>` : ''}
      </div>
    </article>`;
}

async function loadSummary(productId) {
  const url = `/api/v1/product-summary/${encodeURIComponent(productId.trim())}`;
  endpointOut.textContent = `GET ${url}`;
  timingOut.textContent = '';
  result.innerHTML = '<p class="loading">Consultando la Composite API…</p>';

  const started = performance.now();
  try {
    const response = await fetch(url);
    const data = await response.json();
    const elapsed = Math.round(performance.now() - started);

    timingOut.textContent = `· HTTP ${response.status} · ${elapsed} ms`;
    jsonOut.textContent = JSON.stringify(data, null, 2);
    response.ok ? renderSummary(data) : renderFailure(data);
  } catch (err) {
    jsonOut.textContent = '';
    renderFailure({ status: 'ERROR', message: `No se pudo contactar a la Composite API (${err.message}).` });
  }
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  loadSummary(input.value);
});

document.querySelectorAll('.chips button').forEach((chip) => {
  chip.addEventListener('click', () => {
    input.value = chip.dataset.id;
    loadSummary(chip.dataset.id);
  });
});

loadSummary(input.value);
