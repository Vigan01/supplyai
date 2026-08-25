const state = {
    products: [],
    suppliers: [],
    orders: [],
    dashboard: null,
    statistics: null,
    activities: [],
    activeView: "dashboard"
};

const elements = {
    viewTitle: document.querySelector("#viewTitle"),
    viewSelect: document.querySelector("#viewSelect"),
    navButtons: [...document.querySelectorAll(".nav button")],
    views: [...document.querySelectorAll(".view")],
    metrics: document.querySelector("#metrics"),
    statCards: document.querySelector("#statCards"),
    dashboardRows: document.querySelector("#dashboardRows"),
    inventoryRows: document.querySelector("#inventoryRows"),
    alerts: document.querySelector("#alerts"),
    apiStatus: document.querySelector("#apiStatus"),
    refreshButton: document.querySelector("#refreshButton"),
    productForm: document.querySelector("#productForm"),
    productId: document.querySelector("#productId"),
    sku: document.querySelector("#sku"),
    name: document.querySelector("#name"),
    category: document.querySelector("#category"),
    stock: document.querySelector("#stock"),
    reorderPoint: document.querySelector("#reorderPoint"),
    averageDailyDemand: document.querySelector("#averageDailyDemand"),
    unitCost: document.querySelector("#unitCost"),
    supplierId: document.querySelector("#supplierId"),
    resetFormButton: document.querySelector("#resetFormButton"),
    formMessage: document.querySelector("#formMessage"),
    productFilter: document.querySelector("#productFilter"),
    statusFilter: document.querySelector("#statusFilter"),
    categoryFilter: document.querySelector("#categoryFilter"),
    supplierFilter: document.querySelector("#supplierFilter"),
    dashboardStatusFilter: document.querySelector("#dashboardStatusFilter"),
    dashboardSupplierFilter: document.querySelector("#dashboardSupplierFilter"),
    supplierForm: document.querySelector("#supplierForm"),
    supplierEditId: document.querySelector("#supplierEditId"),
    supplierName: document.querySelector("#supplierName"),
    supplierEmail: document.querySelector("#supplierEmail"),
    supplierLeadTimeDays: document.querySelector("#supplierLeadTimeDays"),
    supplierDelayed: document.querySelector("#supplierDelayed"),
    resetSupplierButton: document.querySelector("#resetSupplierButton"),
    supplierMessage: document.querySelector("#supplierMessage"),
    supplierRows: document.querySelector("#supplierRows"),
    orderRows: document.querySelector("#orderRows"),
    orderStatusFilter: document.querySelector("#orderStatusFilter"),
    movementDialog: document.querySelector("#movementDialog"),
    movementForm: document.querySelector("#movementForm"),
    movementProductId: document.querySelector("#movementProductId"),
    movementProductLabel: document.querySelector("#movementProductLabel"),
    movementType: document.querySelector("#movementType"),
    movementQuantity: document.querySelector("#movementQuantity"),
    movementReason: document.querySelector("#movementReason"),
    closeMovementDialog: document.querySelector("#closeMovementDialog"),
    movementRows: document.querySelector("#movementRows"),
    csvFile: document.querySelector("#csvFile"),
    csvMessage: document.querySelector("#csvMessage"),
    statusDistribution: document.querySelector("#statusDistribution"),
    categoryDistribution: document.querySelector("#categoryDistribution"),
    topRisks: document.querySelector("#topRisks"),
    supplierRisk: document.querySelector("#supplierRisk"),
    activityRows: document.querySelector("#activityRows"),
    activityFilter: document.querySelector("#activityFilter")
};

const viewTitles = {
    dashboard: "Operatives Cockpit",
    products: "Produkte verwalten",
    suppliers: "Lieferanten verwalten",
    orders: "Nachbestellungen",
    statistics: "Statistiken",
    activity: "Aktivitätslogs",
    data: "Daten verwalten"
};

const orderLabels = {
    PLANNED: "Geplant",
    ORDERED: "Bestellt",
    DELIVERED: "Geliefert",
    CANCELLED: "Storniert"
};

function badgeClass(status) {
    return String(status).toLowerCase().replaceAll(" ", "-");
}

function money(value) {
    return new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" }).format(Number(value));
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: options.body instanceof FormData ? {} : { "Content-Type": "application/json" },
        ...options
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: "Anfrage fehlgeschlagen." }));
        throw new Error(error.message || "Anfrage fehlgeschlagen.");
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function switchView(view) {
    state.activeView = view;
    elements.viewTitle.textContent = viewTitles[view];
    elements.viewSelect.value = view;
    elements.views.forEach((item) => item.classList.toggle("active", item.id === `view-${view}`));
    elements.navButtons.forEach((button) => button.classList.toggle("active", button.dataset.view === view));
}

function optionList(values, label) {
    return [`<option value="">${label}</option>`, ...values.map((value) => `<option value="${value}">${value}</option>`)].join("");
}

function supplierOptions(label) {
    return [
        `<option value="">${label}</option>`,
        ...state.suppliers.map((supplier) => `<option value="${supplier.id}">${supplier.name}</option>`)
    ].join("");
}

function renderFilterOptions() {
    const statuses = [...new Set(state.products.map((product) => product.status))].sort();
    const categories = [...new Set(state.products.map((product) => product.category))].sort();
    const keep = {
        status: elements.statusFilter.value,
        category: elements.categoryFilter.value,
        supplier: elements.supplierFilter.value,
        dashboardStatus: elements.dashboardStatusFilter.value,
        dashboardSupplier: elements.dashboardSupplierFilter.value
    };
    elements.statusFilter.innerHTML = optionList(statuses, "Alle Status");
    elements.categoryFilter.innerHTML = optionList(categories, "Alle Kategorien");
    elements.supplierFilter.innerHTML = supplierOptions("Alle Lieferanten");
    elements.dashboardStatusFilter.innerHTML = optionList(statuses, "Status");
    elements.dashboardSupplierFilter.innerHTML = supplierOptions("Lieferant");
    elements.statusFilter.value = keep.status;
    elements.categoryFilter.value = keep.category;
    elements.supplierFilter.value = keep.supplier;
    elements.dashboardStatusFilter.value = keep.dashboardStatus;
    elements.dashboardSupplierFilter.value = keep.dashboardSupplier;
}

function renderMetrics() {
    elements.metrics.innerHTML = state.dashboard.metrics.map((metric) => `
        <article class="metric">
            <span>${metric.label}</span>
            <strong>${metric.value}</strong>
            <small>${metric.trend}</small>
        </article>
    `).join("");
}

function productMatches(product, filters) {
    const queryMatch = !filters.query || [
        product.sku,
        product.name,
        product.category,
        product.supplierName
    ].join(" ").toLowerCase().includes(filters.query);
    const statusMatch = !filters.status || product.status === filters.status;
    const categoryMatch = !filters.category || product.category === filters.category;
    const supplierMatch = !filters.supplierId || String(product.supplierId || "") === filters.supplierId;
    return queryMatch && statusMatch && categoryMatch && supplierMatch;
}

function visibleProducts() {
    return state.products.filter((product) => productMatches(product, {
        query: elements.productFilter.value.trim().toLowerCase(),
        status: elements.statusFilter.value,
        category: elements.categoryFilter.value,
        supplierId: elements.supplierFilter.value
    }));
}

function visibleDashboardProducts() {
    return state.products.filter((product) => productMatches(product, {
        query: "",
        status: elements.dashboardStatusFilter.value,
        category: "",
        supplierId: elements.dashboardSupplierFilter.value
    }));
}

function productActionMenu(product) {
    return `
        <select class="action-select" data-id="${product.id}">
            <option value="">Aktion wählen</option>
            <option value="edit">Bearbeiten</option>
            <option value="movement">Bestand buchen</option>
            <option value="reorder">Nachbestellung erstellen</option>
            <option value="duplicate">Duplizieren</option>
            <option value="delete">Löschen</option>
        </select>
    `;
}

function renderDashboardRows() {
    elements.dashboardRows.innerHTML = visibleDashboardProducts().map((product) => `
        <tr>
            <td>${product.sku}</td>
            <td>${product.name}<br><small>${product.supplierName}</small></td>
            <td>${product.stock}</td>
            <td>${product.daysOfCover === 999 ? "kein Bedarf" : `${product.daysOfCover} Tage`}</td>
            <td><span class="badge ${badgeClass(product.status)}">${product.status}</span></td>
            <td>${productActionMenu(product)}</td>
        </tr>
    `).join("");
}

function renderProducts() {
    elements.inventoryRows.innerHTML = visibleProducts().map((product) => `
        <tr>
            <td>${product.sku}</td>
            <td>${product.name}<br><small>${product.supplierName}</small></td>
            <td>${product.category}</td>
            <td>${product.stock}</td>
            <td>${product.daysOfCover === 999 ? "kein Bedarf" : `${product.daysOfCover} Tage`}</td>
            <td>${product.reorderQuantity}</td>
            <td>${money(product.inventoryValue)}</td>
            <td><span class="badge ${badgeClass(product.status)}">${product.status}</span></td>
            <td>${productActionMenu(product)}</td>
        </tr>
    `).join("");
}

function renderAlerts() {
    elements.alerts.innerHTML = state.dashboard.alerts.map((alert) => `
        <div class="alert ${alert.severity}">${alert.message}</div>
    `).join("") || `<div class="alert low">Keine kritischen Warnungen.</div>`;
}

function renderSuppliers() {
    elements.supplierId.innerHTML = [
        `<option value="">Kein Lieferant</option>`,
        ...state.suppliers.map((supplier) => `
            <option value="${supplier.id}">${supplier.name} (${supplier.leadTimeDays} Tage)</option>
        `)
    ].join("");

    elements.supplierRows.innerHTML = state.suppliers.map((supplier) => `
        <tr>
            <td>${supplier.name}</td>
            <td>${supplier.email || "-"}</td>
            <td>${supplier.leadTimeDays} Tage</td>
            <td><span class="badge ${supplier.delayed ? "kritisch" : "stabil"}">${supplier.delayed ? "Verzögert" : "Stabil"}</span></td>
            <td>
                <select class="supplier-action-select" data-id="${supplier.id}">
                    <option value="">Aktion wählen</option>
                    <option value="edit">Bearbeiten</option>
                    <option value="toggle">${supplier.delayed ? "Als stabil markieren" : "Als verzögert markieren"}</option>
                    <option value="delete">Löschen</option>
                </select>
            </td>
        </tr>
    `).join("");
}

function renderOrders() {
    const filter = elements.orderStatusFilter.value;
    const orders = state.orders.filter((order) => !filter || order.status === filter);
    elements.orderRows.innerHTML = orders.map((order) => `
        <tr>
            <td>${order.sku}<br><small>${order.productName}</small></td>
            <td>${order.supplierName}</td>
            <td>${order.quantity}</td>
            <td><span class="badge ${badgeClass(order.status)}">${orderLabels[order.status]}</span></td>
            <td>${order.expectedDeliveryDate || "-"}</td>
            <td>${order.note || "-"}</td>
            <td>
                <select class="order-action-select" data-id="${order.id}">
                    <option value="">Status setzen</option>
                    <option value="ORDERED">Bestellt</option>
                    <option value="DELIVERED">Geliefert</option>
                    <option value="CANCELLED">Storniert</option>
                </select>
            </td>
        </tr>
    `).join("") || `<tr><td colspan="7">Keine Nachbestellungen.</td></tr>`;
}

function renderMovements() {
    const movements = state.dashboard.movements || [];
    elements.movementRows.innerHTML = movements.slice(0, 8).map((movement) => `
        <div class="movement-item">
            <strong>${movement.quantityChange > 0 ? "+" : ""}${movement.quantityChange} ${movement.sku}</strong>
            <span>${movement.reason} · ${new Date(movement.createdAt).toLocaleString("de-DE")}</span>
        </div>
    `).join("") || `<div class="movement-item">Noch keine Bewegungen gebucht.</div>`;
}

function renderStatCards() {
    elements.statCards.innerHTML = state.statistics.cards.map((card) => `
        <article class="metric">
            <span>${card.label}</span>
            <strong>${card.value}</strong>
            <small>${card.detail}</small>
        </article>
    `).join("");
}

function renderDistribution(container, items) {
    const max = Math.max(1, ...items.map((item) => item.value));
    container.innerHTML = items.map((item) => `
        <div class="bar-row">
            <div>
                <strong>${item.label}</strong>
                <span>${item.value}</span>
            </div>
            <div class="bar-track"><span style="width:${Math.max(8, (item.value / max) * 100)}%"></span></div>
        </div>
    `).join("") || `<div class="empty-state">Keine Daten.</div>`;
}

function renderStatistics() {
    renderStatCards();
    renderDistribution(elements.statusDistribution, state.statistics.statusDistribution);
    renderDistribution(elements.categoryDistribution, state.statistics.categoryDistribution);
    elements.topRisks.innerHTML = state.statistics.topRisks.map((risk) => `
        <div class="risk-item">
            <strong>${risk.sku} · ${risk.name}</strong>
            <span>${risk.status} · ${risk.daysOfCover} Tage Reichweite · ${risk.reorderQuantity} nachbestellen</span>
        </div>
    `).join("");
    const supplierRisk = state.statistics.supplierRisk;
    elements.supplierRisk.innerHTML = `
        <div class="risk-meter">
            <strong>${supplierRisk.delayedSuppliers} / ${supplierRisk.totalSuppliers}</strong>
            <span>Lieferanten mit Verzögerung</span>
        </div>
    `;
}

function renderActivity() {
    const type = elements.activityFilter.value;
    const activities = state.activities.filter((activity) => !type || activity.type === type);
    elements.activityRows.innerHTML = activities.map((activity) => `
        <article class="activity-item">
            <div>
                <strong>${activity.title}</strong>
                <span>${activity.details}</span>
            </div>
            <time>${new Date(activity.createdAt).toLocaleString("de-DE")}</time>
        </article>
    `).join("") || `<div class="empty-state">Noch keine passenden Logs.</div>`;
}

function renderAll() {
    renderMetrics();
    renderFilterOptions();
    renderDashboardRows();
    renderProducts();
    renderAlerts();
    renderSuppliers();
    renderOrders();
    renderMovements();
    renderStatistics();
    renderActivity();
}

async function loadData() {
    elements.refreshButton.disabled = true;
    try {
        const [status, dashboard, suppliers, statistics, activities, orders] = await Promise.all([
            api("/api/status"),
            api("/api/dashboard"),
            api("/api/suppliers"),
            api("/api/statistics"),
            api("/api/activity"),
            api("/api/purchase-orders")
        ]);
        state.dashboard = dashboard;
        state.products = dashboard.inventory;
        state.suppliers = suppliers;
        state.statistics = statistics;
        state.activities = activities;
        state.orders = orders;
        renderAll();
        elements.apiStatus.textContent = status.status;
        elements.apiStatus.classList.remove("offline");
    } catch (error) {
        elements.apiStatus.textContent = error.message;
        elements.apiStatus.classList.add("offline");
    } finally {
        elements.refreshButton.disabled = false;
    }
}

function resetForm() {
    elements.productForm.reset();
    elements.productId.value = "";
    elements.formMessage.textContent = "";
}

function fillForm(product) {
    switchView("products");
    elements.productId.value = product.id;
    elements.sku.value = product.sku;
    elements.name.value = product.name;
    elements.category.value = product.category;
    elements.stock.value = product.stock;
    elements.reorderPoint.value = product.reorderPoint;
    elements.averageDailyDemand.value = product.averageDailyDemand;
    elements.unitCost.value = product.unitCost;
    elements.supplierId.value = product.supplierId || "";
    elements.formMessage.textContent = `${product.sku} wird bearbeitet.`;
    elements.sku.focus();
}

function productPayload() {
    return {
        sku: elements.sku.value,
        name: elements.name.value,
        category: elements.category.value,
        stock: Number(elements.stock.value),
        reorderPoint: Number(elements.reorderPoint.value),
        averageDailyDemand: Number(elements.averageDailyDemand.value),
        unitCost: Number(elements.unitCost.value).toFixed(2),
        supplierId: elements.supplierId.value ? Number(elements.supplierId.value) : null
    };
}

function resetSupplierForm() {
    elements.supplierForm.reset();
    elements.supplierEditId.value = "";
    elements.supplierMessage.textContent = "";
}

function supplierPayload() {
    return {
        name: elements.supplierName.value,
        email: elements.supplierEmail.value,
        leadTimeDays: Number(elements.supplierLeadTimeDays.value),
        delayed: elements.supplierDelayed.checked
    };
}

function fillSupplierForm(supplier) {
    switchView("suppliers");
    elements.supplierEditId.value = supplier.id;
    elements.supplierName.value = supplier.name;
    elements.supplierEmail.value = supplier.email || "";
    elements.supplierLeadTimeDays.value = supplier.leadTimeDays;
    elements.supplierDelayed.checked = supplier.delayed;
    elements.supplierMessage.textContent = `${supplier.name} wird bearbeitet.`;
}

function openMovementDialog(product) {
    elements.movementProductId.value = product.id;
    elements.movementProductLabel.textContent = `${product.sku} - ${product.name} · aktueller Bestand ${product.stock}`;
    elements.movementQuantity.value = "";
    elements.movementReason.value = "";
    elements.movementDialog.showModal();
}

async function createPurchaseOrder(product) {
    const deliveryDate = new Date();
    deliveryDate.setDate(deliveryDate.getDate() + 14);
    await api("/api/purchase-orders", {
        method: "POST",
        body: JSON.stringify({
            productId: product.id,
            supplierId: product.supplierId,
            quantity: Math.max(1, product.reorderQuantity),
            expectedDeliveryDate: deliveryDate.toISOString().slice(0, 10),
            note: "Aus Nachbestellvorschlag erstellt"
        })
    });
    switchView("orders");
    await loadData();
}

async function handleAction(action, product) {
    if (action === "edit") {
        fillForm(product);
    }
    if (action === "movement") {
        openMovementDialog(product);
    }
    if (action === "reorder") {
        await createPurchaseOrder(product);
    }
    if (action === "duplicate") {
        switchView("products");
        elements.productId.value = "";
        elements.sku.value = `${product.sku}-COPY`;
        elements.name.value = `${product.name} Kopie`;
        elements.category.value = product.category;
        elements.stock.value = 0;
        elements.reorderPoint.value = product.reorderPoint;
        elements.averageDailyDemand.value = product.averageDailyDemand;
        elements.unitCost.value = product.unitCost;
        elements.supplierId.value = product.supplierId || "";
    }
    if (action === "delete") {
        await api(`/api/products/${product.id}`, { method: "DELETE" });
        await loadData();
    }
}

async function handleActionSelect(event) {
    const select = event.target.closest(".action-select");
    if (!select || !select.value) {
        return;
    }
    const product = state.products.find((item) => item.id === Number(select.dataset.id));
    if (product) {
        await handleAction(select.value, product);
    }
    select.value = "";
}

async function handleSupplierAction(event) {
    const select = event.target.closest(".supplier-action-select");
    if (!select || !select.value) {
        return;
    }
    const supplier = state.suppliers.find((item) => item.id === Number(select.dataset.id));
    if (!supplier) {
        return;
    }
    if (select.value === "edit") {
        fillSupplierForm(supplier);
    }
    if (select.value === "toggle") {
        await api(`/api/suppliers/${supplier.id}`, {
            method: "PUT",
            body: JSON.stringify({ ...supplier, delayed: !supplier.delayed })
        });
        await loadData();
    }
    if (select.value === "delete") {
        await api(`/api/suppliers/${supplier.id}`, { method: "DELETE" });
        await loadData();
    }
    select.value = "";
}

async function handleOrderAction(event) {
    const select = event.target.closest(".order-action-select");
    if (!select || !select.value) {
        return;
    }
    await api(`/api/purchase-orders/${select.dataset.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: select.value })
    });
    select.value = "";
    await loadData();
}

elements.productForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const id = elements.productId.value;
    try {
        await api(id ? `/api/products/${id}` : "/api/products", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(productPayload())
        });
        elements.formMessage.textContent = "Gespeichert.";
        resetForm();
        await loadData();
    } catch (error) {
        elements.formMessage.textContent = error.message;
    }
});

elements.supplierForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const id = elements.supplierEditId.value;
    try {
        await api(id ? `/api/suppliers/${id}` : "/api/suppliers", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(supplierPayload())
        });
        elements.supplierMessage.textContent = "Gespeichert.";
        resetSupplierForm();
        await loadData();
    } catch (error) {
        elements.supplierMessage.textContent = error.message;
    }
});

elements.movementForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const productId = elements.movementProductId.value;
    try {
        await api(`/api/products/${productId}/movements`, {
            method: "POST",
            body: JSON.stringify({
                type: elements.movementType.value,
                quantity: Number(elements.movementQuantity.value),
                reason: elements.movementReason.value
            })
        });
        elements.movementDialog.close();
        await loadData();
    } catch (error) {
        elements.movementProductLabel.textContent = error.message;
    }
});

elements.csvFile.addEventListener("change", async () => {
    const file = elements.csvFile.files[0];
    if (!file) {
        return;
    }
    const formData = new FormData();
    formData.append("file", file);
    try {
        const result = await api("/api/products/csv", { method: "POST", body: formData });
        elements.csvMessage.textContent = `${result.imported} Zeilen importiert. ${result.errors.length} Fehler.`;
        await loadData();
    } catch (error) {
        elements.csvMessage.textContent = error.message;
    } finally {
        elements.csvFile.value = "";
    }
});

elements.navButtons.forEach((button) => button.addEventListener("click", () => switchView(button.dataset.view)));
elements.viewSelect.addEventListener("change", () => switchView(elements.viewSelect.value));
elements.refreshButton.addEventListener("click", loadData);
elements.resetFormButton.addEventListener("click", resetForm);
elements.resetSupplierButton.addEventListener("click", resetSupplierForm);
elements.dashboardRows.addEventListener("change", handleActionSelect);
elements.inventoryRows.addEventListener("change", handleActionSelect);
elements.supplierRows.addEventListener("change", handleSupplierAction);
elements.orderRows.addEventListener("change", handleOrderAction);
elements.closeMovementDialog.addEventListener("click", () => elements.movementDialog.close());
elements.activityFilter.addEventListener("change", renderActivity);
elements.orderStatusFilter.addEventListener("change", renderOrders);

[
    elements.productFilter,
    elements.statusFilter,
    elements.categoryFilter,
    elements.supplierFilter
].forEach((input) => input.addEventListener("input", renderProducts));

[
    elements.dashboardStatusFilter,
    elements.dashboardSupplierFilter
].forEach((input) => input.addEventListener("input", renderDashboardRows));

loadData();
