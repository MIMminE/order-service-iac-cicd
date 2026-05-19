const loginForm = document.querySelector("#login-form");
const productForm = document.querySelector("#product-form");
const orderForm = document.querySelector("#order-form");
const logoutButton = document.querySelector("#logout-button");
const refreshButton = document.querySelector("#refresh-products");
const sessionState = document.querySelector("#session-state");
const productList = document.querySelector("#product-list");
const productSelect = document.querySelector("#product-select");
const productCount = document.querySelector("#product-count");
const stockTotal = document.querySelector("#stock-total");
const lastOrder = document.querySelector("#last-order");
const orderResult = document.querySelector("#order-result");
const opsState = document.querySelector("#ops-state");

let accessToken = window.localStorage.getItem("orderServiceToken") || "";
let products = [];

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const payload = Object.fromEntries(new FormData(loginForm).entries());
  const response = await fetchJson("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  accessToken = response.accessToken;
  window.localStorage.setItem("orderServiceToken", accessToken);
  setSession(true);
  await loadProducts();
});

logoutButton.addEventListener("click", () => {
  accessToken = "";
  window.localStorage.removeItem("orderServiceToken");
  setSession(false);
});

refreshButton.addEventListener("click", loadProducts);

productForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  setOpsState("등록 중");
  try {
    const payload = Object.fromEntries(new FormData(productForm).entries());
    payload.price = Number(payload.price);
    payload.stock = Number(payload.stock);

    await fetchJson("/products", {
      method: "POST",
      body: JSON.stringify(payload),
      auth: true
    });
    productForm.reset();
    productForm.elements.name.value = "Limited Notebook";
    productForm.elements.price.value = "18000";
    productForm.elements.stock.value = "60";
    await loadProducts();
    setOpsState("Ready");
  } catch (error) {
    handleActionError("상품 등록", error);
  }
});

orderForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  setOpsState("주문 처리 중");
  try {
    const formData = new FormData(orderForm);
    const payload = {
      items: [
        {
          productId: Number(formData.get("productId")),
          quantity: Number(formData.get("quantity"))
        }
      ]
    };

    const created = await fetchJson("/orders", {
      method: "POST",
      body: JSON.stringify(payload),
      auth: true
    });
    const detail = await fetchJson(`/orders/${created.orderId}`, { auth: true });
    lastOrder.textContent = `#${detail.orderId}`;
    orderResult.innerHTML = `
      <strong>주문 #${detail.orderId} 생성 완료</strong>
      총 주문 금액 ${formatMoney(detail.totalAmount)} / 상태 ${detail.status}<br>
      ${detail.items.map((item) => `상품 ${item.productId} x ${item.quantity}`).join(", ")}
    `;
    await loadProducts();
    setOpsState("Ready");
  } catch (error) {
    handleActionError("주문 생성", error);
  }
});

async function loadProducts() {
  const response = await fetchJson("/products");
  products = response.items || [];
  renderProducts();
  setSession(Boolean(accessToken));
}

function renderProducts() {
  productList.innerHTML = products.map((product) => `
    <article class="product-card">
      <div>
        <h3>${escapeHtml(product.name)}</h3>
        <span>${formatMoney(product.price)}</span>
      </div>
      <strong class="stock">${product.stock.toLocaleString("ko-KR")}개</strong>
    </article>
  `).join("");

  productSelect.innerHTML = products.map((product) => (
    `<option value="${product.id}">${escapeHtml(product.name)} / 재고 ${product.stock}</option>`
  )).join("");

  productCount.textContent = products.length.toLocaleString("ko-KR");
  stockTotal.textContent = products.reduce((sum, product) => sum + product.stock, 0).toLocaleString("ko-KR");
}

function setOpsState(value) {
  opsState.textContent = value;
}

async function fetchJson(url, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.auth ? { Authorization: `Bearer ${accessToken}` } : {})
  };
  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    throw await parseApiError(response);
  }
  return response.status === 204 ? {} : response.json();
}

async function parseApiError(response) {
  const fallback = response.status === 401
    ? "세션이 만료되었습니다. 다시 로그인해 주세요."
    : "요청을 처리하지 못했습니다.";

  try {
    const payload = await response.json();
    const message = payload?.error?.message || payload?.message || fallback;
    const error = new Error(message === "Unauthorized" ? fallback : message);
    error.status = response.status;
    return error;
  } catch {
    const error = new Error(fallback);
    error.status = response.status;
    return error;
  }
}

function handleActionError(action, error) {
  setOpsState("확인 필요");
  if (error.status === 401) {
    accessToken = "";
    window.localStorage.removeItem("orderServiceToken");
    setSession(false);
  }
  orderResult.textContent = `${action} 실패: ${error.message}`;
}

function setSession(isSignedIn) {
  sessionState.textContent = isSignedIn ? "Admin session active" : "Signed out";
  document.body.classList.toggle("is-signed-in", isSignedIn);
}

function formatMoney(value) {
  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: "KRW",
    maximumFractionDigits: 0
  }).format(value);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

setSession(Boolean(accessToken));
loadProducts().catch((error) => {
  productList.innerHTML = `<div class="result-box">${escapeHtml(error.message)}</div>`;
  setOpsState("확인 필요");
});
