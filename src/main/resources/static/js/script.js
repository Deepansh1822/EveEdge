/**
 * JWT Token Management
 */
function getJwtToken() {
	return localStorage.getItem('jwtToken');
}

function isAuthenticated() {
	return !!getJwtToken();
}

function getUsername() {
	return localStorage.getItem('username');
}

window.setIconButtonLoading = function (btn) {
	if (!btn) return;
	btn.dataset.originalHtml = btn.innerHTML;
	btn.disabled = true;
	btn.innerHTML = '<i data-lucide="loader-2" class="animate-spin" size="18"></i>';
	if (window.lucide) window.lucide.createIcons();
};

window.resetIconButtonLoading = function (btn) {
	if (!btn) return;
	btn.innerHTML = btn.dataset.originalHtml || btn.innerHTML;
	btn.disabled = false;
	// if (window.lucide) window.lucide.createIcons();
};

/**
 * Global Button Loading State Helpers
 */
window.setButtonLoading = function (btn) {
	if (!btn) return;
	btn.dataset.originalText = btn.innerHTML;
	btn.disabled = true;
	// maintain width if possible or just replace content
	btn.innerHTML = '<i data-lucide="loader-2" class="animate-spin" style="margin-right: 8px;" size="18"></i> Processing...';
	if (window.lucide) window.lucide.createIcons();
};

window.resetButtonLoading = function (btn) {
	if (!btn) return;
	btn.innerHTML = btn.dataset.originalText || btn.innerText;
	btn.disabled = false;
	// if (window.lucide) window.lucide.createIcons();
};

function getUserRoles() {
	const roles = localStorage.getItem('userRoles');
	return roles ? JSON.parse(roles) : [];
}

function isAdmin() {
	const roles = getUserRoles();
	return roles.some(role => role.authority === 'ROLE_ADMIN');
}

function logout() {
	localStorage.removeItem('jwtToken');
	localStorage.removeItem('username');
	localStorage.removeItem('userRoles');
	// Clear HttpOnly cookie via API
	fetch('/api/auth/logout', { method: 'POST' }).finally(() => {
		window.location.href = '/login';
	});
}

// Add JWT token to fetch requests
function fetchWithAuth(url, options = {}) {
	const token = getJwtToken();
	if (!token) {
		window.location.href = '/login';
		return Promise.reject('No authentication token');
	}

	const headers = {
		...options.headers,
		'Authorization': `Bearer ${token}`
	};

	return fetch(url, { ...options, headers });
}

/**
 * Global store for events to avoid complex stringification in HTML
 */
let loadedEvents = {};
let currentActiveCategory = null; // Tracks {id, name} when categorical events modal is open

/**
 * Animates a counter from 0 to the target value
 */
function animateCounter(element, target, duration = 1000) {
	if (!element) return;
	let startTimestamp = null;
	const step = (timestamp) => {
		if (!startTimestamp) startTimestamp = timestamp;
		const progress = Math.min((timestamp - startTimestamp) / duration, 1);
		element.innerText = Math.floor(progress * target);
		if (progress < 1) window.requestAnimationFrame(step);
		else element.innerText = target;
	};
	window.requestAnimationFrame(step);
}

/**
 * Safely initializes Lucide icons
 */
function refreshIcons() {
	if (window.lucide) {
		window.lucide.createIcons();
	}
}

/**
 * Modal Controls
 */
function openModal(id) {
	document.getElementById(id)?.classList.add('active');
}

function closeModal(id) {
	document.getElementById(id)?.classList.remove('active');
}

/**
 * Custom Toast Notification
 */
function showToast(title, message, type = 'success') {
	const container = document.getElementById('toast-container');
	if (!container) return;

	const toast = document.createElement('div');
	toast.className = `toast ${type}`;

	let icon = 'check-circle';
	if (type === 'error') icon = 'alert-circle';
	if (type === 'info') icon = 'info';
	if (type === 'warning') icon = 'alert-triangle';

	toast.innerHTML = `
        <div class="toast-icon" style="display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 50%;">
            <i data-lucide="${icon}" size="18"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
    `;

	container.appendChild(toast);
	if (window.lucide) window.lucide.createIcons();

	// Animate in
	setTimeout(() => toast.classList.add('active'), 10);

	// Remove after 4 seconds
	setTimeout(() => {
		toast.classList.remove('active');
		setTimeout(() => toast.remove(), 400);
	}, 4000);
}

/**
 * Custom Confirmation Modal
 */
let confirmCallback = null;
function showConfirm(message, onConfirm) {
	const modal = document.getElementById('confirm-modal');
	const text = document.getElementById('confirm-modal-text');
	if (!modal || !text) return;

	text.innerText = message;
	confirmCallback = onConfirm;
	openModal('confirm-modal');

	// Ensure listeners are only attached once
	const okBtn = document.getElementById('confirm-ok-btn');
	const cancelBtn = document.getElementById('confirm-cancel-btn');

	if (okBtn && !okBtn.dataset.listener) {
		okBtn.addEventListener('click', async () => {
			if (confirmCallback) {
				// Show loading on the OK button
				const OriginalText = okBtn.innerText;
				okBtn.disabled = true;
				okBtn.innerHTML = '<i data-lucide="loader-2" class="animate-spin" size="18"></i>';
				if (window.lucide) window.lucide.createIcons();

				try {
					await confirmCallback();
				} catch (e) {
					console.error(e);
				} finally {
					// Reset button (though usually modal closes)
					okBtn.innerHTML = OriginalText;
					okBtn.disabled = false;
					confirmCallback = null;
				}
			}
			closeModal('confirm-modal');
		});
		okBtn.dataset.listener = 'true';
	}

	if (cancelBtn && !cancelBtn.dataset.listener) {
		cancelBtn.addEventListener('click', () => {
			confirmCallback = null;
			closeModal('confirm-modal');
		});
		cancelBtn.dataset.listener = 'true';
	}
}

/**
 * Cart Logic
 */
function addToCart(eventId) {
	const event = loadedEvents[eventId];
	if (!event) return;

	if (event.availableSeats <= 0) {
		showToast("Sold Out", "Sorry, this event is already sold out.", "error");
		return;
	}

	// NEW: If event is free, open the Quick Registration modal instead of adding to cart
	if (!event.paidEvent) {
		document.getElementById('quick-register-form').reset();
		document.getElementById('register-event-id').value = event.eventId;
		document.getElementById('register-event-name').innerText = event.title;
		openModal('register-modal');
		return;
	}

	let cart = JSON.parse(localStorage.getItem('event_cart') || '[]');
	const existingIndex = cart.findIndex(item => item.eventId === event.eventId);

	if (existingIndex !== -1) {
		const existingItem = cart[existingIndex];
		// Synchronize availableSeats just in case it was missing or old
		existingItem.availableSeats = event.availableSeats;

		if (existingItem.quantity < event.availableSeats) {
			existingItem.quantity += 1;
			localStorage.setItem('event_cart', JSON.stringify(cart));
			showToast("Selection Updated", `Increased quantity for ${event.title}.`, "success");
			if (typeof updateCartCount === 'function') updateCartCount();
		} else {
			showToast("Limit Reached", `You cannot book more than ${event.availableSeats} tickets for this event.`, "info");
		}
		return;
	}

	cart.push({
		eventId: event.eventId,
		title: event.title,
		price: event.price,
		location: event.location,
		eventDate: event.eventDate,
		banner_image: event.banner_image,
		availableSeats: event.availableSeats, // Crucial for cart page validation
		quantity: 1
	});

	localStorage.setItem('event_cart', JSON.stringify(cart));
	showToast("Ticket Selected", `${event.title} has been added to your bookings.`, "success");
	if (typeof updateCartCount === 'function') updateCartCount();
}

/**
 * Updates the global cart count badge
 */
function updateCartCount() {
	const badges = [
		document.getElementById('cart-count-badge'),
		document.getElementById('mobile-cart-count-badge') // Mobile nav counter
	];

	const cart = JSON.parse(localStorage.getItem('event_cart') || '[]');
	const count = cart.reduce((sum, item) => sum + (parseInt(item.quantity) || 1), 0);

	badges.forEach(badge => {
		if (badge) {
			if (count > 0) {
				badge.innerText = count;
				badge.style.setProperty('display', 'flex', 'important');
			} else {
				badge.style.setProperty('display', 'none', 'important');
			}
		}
	});
}

/**
 * Handles direct registration for free events
 */
async function handleQuickRegister(e) {
	e.preventDefault();
	const eventId = document.getElementById('register-event-id').value;
	const name = document.getElementById('reg-cust-name').value;
	const email = document.getElementById('reg-cust-email').value;
	const mobile = document.getElementById('reg-cust-mobile').value;
	const org = document.getElementById('reg-cust-org').value;
	const userType = document.getElementById('reg-cust-type').value;

	const submitBtn = e.target.querySelector('button[type="submit"]');
	window.setButtonLoading(submitBtn);

	const bookingData = {
		event: { eventId: parseInt(eventId) },
		customerName: name,
		customerEmail: email,
		mobileNumber: mobile,
		organization: org,
		userType: userType,
		quantity: 1,
		totalAmount: 0.0,
		status: "Confirmed",
		paymentMode: "Free Registration"
	};

	try {
		const res = await fetchWithAuth('/api/bookings/add', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(bookingData)
		});

		if (res.ok) {
			showToast("Registration Successful", "You have successfully registered for this event. Your pass is ready!", "success");

			// Clear form and close modal
			const regForm = document.getElementById('quick-register-form');
			if (regForm) regForm.reset();
			closeModal('register-modal');

			setTimeout(() => {
				window.location.href = '/tickets';
			}, 1500);
		} else {
			const err = await res.text();
			showToast("Error", "Registration failed: " + err, "error");
		}
	} catch (err) {
		console.error("Reg error:", err);
		showToast("Error", "Connection failed.", "error");
	} finally {
		window.resetButtonLoading(submitBtn);
	}
}

/**
 * Category Actions: Edit & Delete
 */
function editCategory(btn, catId) {
	if (typeof btn === 'number') { catId = btn; btn = null; }
	if (btn) window.setIconButtonLoading(btn);

	fetch(`/api/categories/get/${catId}`)
		.then(res => res.json())
		.then(cat => {
			document.getElementById('category-modal-title').innerText = "Update Category";
			document.getElementById('category-submit-btn').innerText = "Update Category";
			document.getElementById('cat-id-input').value = cat.categId;
			document.getElementById('cat-name-input').value = cat.catName;
			document.getElementById('cat-desc-input').value = cat.description || '';
			document.getElementById('cat-color-input').value = cat.catColor || '#4f46e5';

			// Show image preview
			showCategoryImagePreview(cat.catIconBase64);

			closeModal('category-events-modal');
			openModal('category-modal');
		})
		.finally(() => {
			if (btn) window.resetIconButtonLoading(btn);
		});
}

async function deleteCategory(btn, catId) {
	if (typeof btn === 'number') { catId = btn; btn = null; }
	if (btn) window.setIconButtonLoading(btn);

	try {
		const res = await fetch(`/api/categories/get/${catId}`);
		const category = await res.json();
		const eventCount = category.events ? category.events.length : 0;

		let message = "Are you sure you want to delete this category?";
		if (eventCount > 0) {
			message = `Are you sure you want to delete this category? It contains ${eventCount} event(s) which will also be deleted permanently.`;
		}

		showConfirm(message, () => {
			return fetch(`/api/categories/delete/${catId}`, { method: 'DELETE' })
				.then(res => {
					if (res.ok) {
						closeModal('category-events-modal');
						loadCategories();
						loadStats();
						loadEvents();
						showToast("Category Deleted", "The category and its associated events have been removed successfully.", "success");
					} else {
						showToast("Error", "Failed to delete category.", "error");
					}
				});
		});
	} catch (e) {
		console.error("Error checking category events:", e);
		showToast("Error", "Could not verify category data before deletion.", "error");
	} finally {
		if (btn) window.resetIconButtonLoading(btn);
	}
}

/**
 * Event Actions: Edit & Delete
 */
function editEvent(eventId) {
	fetch(`/api/events/get/${eventId}`)
		.then(res => res.json())
		.then(event => {
			document.getElementById('event-modal-title').innerText = "Update Event";
			document.getElementById('event-submit-btn').innerText = "Update Event";
			document.getElementById('event-id-input').value = event.eventId;
			document.getElementById('event-title-input').value = event.title;
			document.getElementById('event-cat-select').value = event.category ? event.category.categId : '';
			document.getElementById('event-price-input').value = event.price;
			document.getElementById('event-loc-input').value = event.location;
			document.getElementById('event-date-input').value = event.eventDate;
			document.getElementById('event-start-time-input').value = event.startTime || '';
			document.getElementById('event-end-time-input').value = event.endTime || '';
			document.getElementById('event-desc-input').value = event.description || '';
			document.getElementById('event-seats-input').value = event.maxSeats;

			const paidCheckbox = document.getElementById('event-paid-input');
			if (paidCheckbox) {
				paidCheckbox.checked = event.paidEvent;
				// Trigger change to update price input state
				paidCheckbox.dispatchEvent(new Event('change'));
			}

			// If paid event, ensure price is set correctly (it might have been cleared by the change event if 0)
			if (event.paidEvent) {
				document.getElementById('event-price-input').value = event.price;
			} else {
				// If unpaid, force UI to zero/free
				document.getElementById('event-price-input').value = "0";
			}

			// Show image preview
			showEventBannerPreview(event.bannerBase64);

			closeModal('category-events-modal');
			openModal('event-modal');
		});
}

function deleteEvent(eventId) {
	showConfirm("Are you sure you want to delete this event?", () => {
		const event = loadedEvents[eventId];

		// Use event's category if available, otherwise fallback to the currently open category context
		const catId = (event && event.category) ? event.category.categId : (currentActiveCategory ? currentActiveCategory.id : null);
		const catName = (event && event.category) ? event.category.catName : (currentActiveCategory ? currentActiveCategory.name : 'General');

		return fetch(`/api/events/delete/${eventId}`, { method: 'DELETE' })
			.then(res => {
				if (res.ok) {
					// Refresh background data
					loadEvents();
					loadStats();
					loadCategories();

					// If we are currently inside a category modal, refresh it DO NOT CLOSE IT
					const modal = document.getElementById('category-events-modal');
					if (modal && modal.classList.contains('active') && (catId || currentActiveCategory)) {
						// Use explicit context if available
						const refreshId = catId || currentActiveCategory.id;
						const refreshName = catName || currentActiveCategory.name;
						openCategoryEvents(refreshId, refreshName);
					}
					showToast("Event Deleted", "The event has been permanently removed from the system.", "success");
				} else {
					showToast("Action Failed", "Failed to delete event. It might have active bookings that prevent removal.", "error");
				}
			});
	});
}

/**
 * Helper to generate Event Card HTML
 */
/**
 * Format time to 12-hour AM/PM
 */
function formatTime12Hour(timeStr) {
	if (!timeStr) return '';
	try {
		const [hours, minutes] = timeStr.split(':');
		let h = parseInt(hours);
		const ampm = h >= 12 ? 'PM' : 'AM';
		h = h % 12;
		h = h ? h : 12; // the hour '0' should be '12'
		return `${h}:${minutes} ${ampm}`;
	} catch (e) { return timeStr; }
}

function createEventCardHtml(event) {
	// Store event in global store
	loadedEvents[event.eventId] = event;

	const bannerHtml = event.bannerBase64
		? `data:image/jpeg;base64,${event.bannerBase64}`
		: `https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&q=80&w=400`;

	let statusColor = 'var(--primary)';
	if (event.displayStatus === 'Live') statusColor = '#10b981';
	if (event.displayStatus === 'Passed') statusColor = '#ef4444';
	if (event.displayStatus === 'Cancelled') statusColor = '#f59e0b';

	const isSoldOut = event.availableSeats <= 0;
	const isInactive = event.displayStatus === 'Passed' || event.displayStatus === 'Cancelled';

	// Format Time Range
	let timeDisplay = '';
	if (event.startTime) {
		const start = formatTime12Hour(event.startTime);
		const end = event.endTime ? formatTime12Hour(event.endTime) : '';
		timeDisplay = end ? `${start} - ${end}` : start;
	}

	return `
        <div class="event-card">
            <!-- Actions removed as per user request -->
            <div class="event-banner" onclick="window.location.href='/event-details?id=${event.eventId}'">
                <img src="${bannerHtml}" alt="${event.title}">
                <div class="event-price">${event.paidEvent ? `₹${event.price}` : 'Free'}</div>
                <div class="event-status-badge" style="background: ${isSoldOut && !isInactive ? '#64748b' : statusColor}; ${event.displayStatus === 'Cancelled' ? 'display:none;' : ''}">
                    ${isSoldOut && !isInactive ? 'Sold Out' : event.displayStatus}
                </div>
                <div class="lifecycle-badge ${event.status.toLowerCase()} ${event.displayStatus === 'Cancelled' ? 'cancelled-status' : ''}">
                    ${event.status}
                </div>
            </div>
            <div class="event-details" onclick="window.location.href='/event-details?id=${event.eventId}'">
                <div class="event-tags-row">
                    <div class="event-category-tag">${event.category ? event.category.catName : 'General'}</div>
                    <div class="event-seats-tag ${event.availableSeats === 0 ? 'low-seats' : ''}">
                        <i data-lucide="users"></i> ${event.availableSeats} / ${event.maxSeats} left
                    </div>
                </div>
                <h3 class="event-title">${event.title}</h3>
                <div class="event-meta">
                    <div class="event-meta-row">
                        <i data-lucide="calendar"></i> 
                        <span>${event.eventDate || 'Date TBD'}</span>
                    </div>
                    ${timeDisplay ? `
                    <div class="event-meta-row time-row">
                        <i data-lucide="clock"></i> 
                        <span>${timeDisplay}</span>
                    </div>
                    ` : ''}
                    <div class="event-meta-row location-row">
                        <i data-lucide="map-pin"></i> 
                        <span class="loc-text">${event.location}</span>
                    </div>
                </div>
                <p class="event-desc">${event.description || 'No description available.'}</p>
                <button class="book-btn" onclick="event.stopPropagation(); addToCart(${event.eventId})" 
                    ${(isInactive || isSoldOut) ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : ''}>
                    ${event.displayStatus === 'Passed' ? 'Event Ended' : (event.displayStatus === 'Cancelled' ? 'Cancelled' : (isSoldOut ? 'Sold Out' : (event.paidEvent ? 'Book Now' : 'Register')))}
                </button>
            </div>
        </div>
    `;
}

/**
 * Opens category events modal
 */
function openCategoryEvents(catId, catName) {
	// Store current context for deletion/update refreshes
	currentActiveCategory = { id: catId, name: catName };

	const titleEl = document.getElementById('cat-events-title');
	const listEl = document.getElementById('category-events-list');
	const actionsEl = document.getElementById('category-actions');

	if (titleEl) titleEl.innerText = `${catName} Events`;

	// Show loading spinner
	if (listEl) {
		listEl.innerHTML = `
			<div style="grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 5rem 2rem; width: 100%;">
				<i data-lucide="loader-2" class="animate-spin" size="48" style="color: var(--primary); margin-bottom: 1.5rem;"></i>
				<p style="color: var(--text-muted); font-size: 1.1rem; font-weight: 500;">Fetching events for ${catName}...</p>
			</div>
		`;
		refreshIcons();
	}

	// Check admin role for actions
	const rolesStr = localStorage.getItem('userRoles');
	const roles = rolesStr ? JSON.parse(rolesStr) : [];
	const isAdmin = Array.isArray(roles) && roles.some(role => role.authority === 'ROLE_ADMIN');

	if (actionsEl) {
		if (isAdmin) {
			actionsEl.innerHTML = `
			<button class="btn-outline admin-only" onclick="editCategory(this, ${catId})">
				<i data-lucide="edit-3" size="16"></i> Update Category
			</button>
			<button class="btn-outline danger admin-only" onclick="deleteCategory(this, ${catId})">
				<i data-lucide="trash-2" size="16"></i> Delete Category
			</button>
		`;
			// Re-run applyUserVisibility just in case, though we manually checked
		} else {
			actionsEl.innerHTML = '';
		}
	}

	openModal('category-events-modal');

	fetch(`/api/categories/get/${catId}`)
		.then(res => res.json())
		.then(category => {
			if (!category.events || category.events.length === 0) {
				listEl.innerHTML = `
                    <div class="empty-state" style="grid-column: 1 / -1; width: 100%; border: none; box-shadow: none;">
                        <div class="empty-state-icon">
                            <i data-lucide="calendar-x" size="48"></i>
                        </div>
                        <h3 class="empty-state-title">No Events Yet</h3>
                        <p class="empty-state-text">We couldn't find any events in the <b>${catName}</b> category. Check back later or explore other categories!</p>
                    </div>
                `;
				refreshIcons();
			} else {
				listEl.innerHTML = category.events.map(event => {
					// Ensure the event knows its category name for the card display
					if (!event.category) {
						event.category = { catName: category.catName };
					}
					return createEventCardHtml(event);
				}).join('');
				refreshIcons();
			}
		})
		.catch(err => {
			console.error("Error loading category events:", err);
			listEl.innerHTML = `
				<div style="grid-column: 1 / -1; text-align: center; padding: 5rem 2rem; width: 100%; color: var(--accent);">
					<i data-lucide="alert-circle" size="48" style="margin-bottom: 1.5rem;"></i>
					<p style="font-size: 1.1rem; font-weight: 500;">Failed to load events. Please try again.</p>
				</div>
			`;
			refreshIcons();
		});
}

/**
 * Fetches and displays event categories
 */
function loadCategories() {
	const container = document.querySelector("#category-container");
	if (container) {
		container.innerHTML = `
			<div style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 2rem; width: 100%; min-height: 150px;">
				<i data-lucide="loader-2" class="animate-spin" size="40" style="color: var(--primary); margin-bottom: 1rem;"></i>
				<p style="color: var(--text-muted); font-size: 0.9rem;">Loading categories...</p>
			</div>
		`;
		refreshIcons();
	}
	fetch("/api/categories/get")
		.then(res => res.json())
		.then(categories => {
			const container = document.querySelector("#category-container");
			if (!container) return;

			if (categories.length === 0) {
				container.innerHTML = `
                    <div class="empty-state" style="min-width: 100%;">
                        <div class="empty-state-icon">
                            <i data-lucide="folder-x" size="40"></i>
                        </div>
                        <h3 class="empty-state-title">No Categories</h3>
                        <p class="empty-state-text">No categories are currently available.</p>
                    </div>
                `;
			} else {
				container.innerHTML = categories.map(cat => {
					const iconHtml = cat.catIcon
						? `<img src="data:image/png;base64,${cat.catIcon}" style="width: 32px; height: 32px; object-fit: contain; border-radius: 6px;">`
						: `<i data-lucide="layers" size="32"></i>`;

					return `
                    <div class="category-card" style="--cat-color: ${cat.catColor || '#4f46e5'}" onclick="openCategoryEvents(${cat.categId}, '${cat.catName}')">
                        <div class="category-icon">
                            ${iconHtml}
                        </div>
                        <div class="category-info">
                            <h3>${cat.catName}</h3>
                            <p>${cat.description || 'Explore events'}</p>
                        </div>
                    </div>
                `}).join('');
			}

			const select = document.querySelector("#event-cat-select");
			if (select) {
				const currentVal = select.value;
				select.innerHTML = '<option value="">Select a category</option>' +
					categories.map(cat => `<option value="${cat.categId}">${cat.catName}</option>`).join('');
				select.value = currentVal;
			}

			refreshIcons();
		})
		.catch(err => console.error("Error loading categories:", err));
}

/**
 * Fetches and displays events (Top 10 by bookings)
 */
function loadEvents() {
	const container = document.querySelector("#event-list");
	if (container) {
		container.innerHTML = `
			<div style="grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 4rem; width: 100%;">
				<i data-lucide="loader-2" class="animate-spin" size="40" style="color: var(--primary); margin-bottom: 1rem;"></i>
				<p style="color: var(--text-muted); font-size: 0.95rem;">Finding top events for you...</p>
			</div>
		`;
		refreshIcons();
	}
	fetch("/api/events/top")
		.then(res => res.json())
		.then(events => {
			const container = document.querySelector("#event-list");
			if (!container) return;

			if (events.length === 0) {
				container.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <i data-lucide="calendar-off" size="40"></i>
                        </div>
                        <h3 class="empty-state-title">No Events Found</h3>
                        <p class="empty-state-text">There are no events to display right now.</p>
                    </div>
                `;
			} else {
				container.innerHTML = events.map(event => createEventCardHtml(event)).join('');
			}
			refreshIcons();
		})
		.catch(err => console.error("Error loading events:", err));
}

/**
 * Stats Loader
 */
function loadStats() {
	const stats = [
		{ id: "#stat-categories", url: "/api/categories/get/count" },
		{ id: "#stat-events", url: "/api/events/get/count" },
		{ id: "#stat-bookings", url: "/api/bookings/get/count", isJson: true },
		{ id: "#stat-pending", url: "/admin/api/pending-payments/count", isJson: true }
	];

	stats.forEach(stat => {
		fetchWithAuth(stat.url)
			.then(res => stat.isJson ? res.json() : res.text())
			.then(data => {
				const el = document.querySelector(stat.id);
				const count = stat.isJson ? (data.count || 0) : (parseInt(data) || 0);
				if (el) animateCounter(el, count);
			})
			.catch(err => console.error(`Error fetching ${stat.id}:`, err));
	});
}

/**
 * High-Quality Image Resizer
 */
async function resizeImage(file, maxWidth = 64, maxHeight = 64) {
	return new Promise((resolve) => {
		const reader = new FileReader();
		reader.onload = (e) => {
			const img = new Image();
			img.onload = () => {
				const canvas = document.createElement('canvas');
				let width = img.width;
				let height = img.height;

				// Proportional scaling: stay within bounds while maintaining aspect ratio
				const scaleRatio = Math.min(maxWidth / width, maxHeight / height, 1);
				width = width * scaleRatio;
				height = height * scaleRatio;

				canvas.width = width;
				canvas.height = height;
				const ctx = canvas.getContext('2d');

				// Enable high-quality smoothing
				ctx.imageSmoothingEnabled = true;
				ctx.imageSmoothingQuality = 'high';

				ctx.drawImage(img, 0, 0, width, height);
				// Quality 0.9 for crystal clear images without massive file sizes
				resolve(canvas.toDataURL('image/jpeg', 0.9).split(',')[1]);
			};
			img.src = e.target.result;
		};
		reader.readAsDataURL(file);
	});
}

/**
 * Form Submissions
 */
async function handleCategorySubmit(e) {
	e.preventDefault();
	const form = e.target;
	const catId = document.getElementById('cat-id-input').value;
	const isUpdate = catId !== "";

	const iconFile = document.getElementById('cat-icon-input').files[0];
	let iconBase64 = null;

	if (iconFile) {
		iconBase64 = await resizeImage(iconFile);
	}

	const data = {
		catName: document.getElementById('cat-name-input').value,
		description: document.getElementById('cat-desc-input').value,
		catColor: document.getElementById('cat-color-input').value,
		catIcon: iconBase64
	};
	if (isUpdate) data.categId = parseInt(catId);

	const submitBtn = form.querySelector('button[type="submit"]');
	window.setButtonLoading(submitBtn);

	const url = isUpdate ? `/api/categories/update/${catId}` : "/api/categories/add";
	const method = isUpdate ? "PUT" : "POST";

	fetch(url, {
		method: method,
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(data)
	})
		.then(async res => {
			if (res.ok) {
				closeModal('category-modal');
				form.reset();
				loadCategories();
				loadStats();
				showToast(isUpdate ? "Category Updated" : "Category Added", isUpdate ? "The category details have been updated successfully." : "The new category has been added and is ready for use.", "success");
			} else {
				const errorText = await res.text();
				showToast("Error", "Operation failed: " + (errorText || res.status), "error");
			}
		})
		.catch(err => {
			console.error("Error:", err);
			showToast("Error", "An unexpected error occurred.", "error");
		})

		.finally(() => {
			window.resetButtonLoading(submitBtn);
		});
}

async function handleEventSubmit(e) {
	e.preventDefault();
	const form = e.target;
	const eventId = document.getElementById('event-id-input').value;
	const isUpdate = eventId !== "";

	const bannerFile = document.getElementById('event-banner-input').files[0];
	let bannerBase64 = null;

	if (bannerFile) {
		// Set to Full HD (1920x1080) for absolute clarity on the details page
		bannerBase64 = await resizeImage(bannerFile, 1920, 1080);
	}

	const eventDateStr = document.getElementById('event-date-input').value;
	const maxSeats = parseInt(document.getElementById('event-seats-input').value);

	if (maxSeats <= 0) {
		showToast("Invalid Input", "Seats must be at least 1.", "error");
		return;
	}

	let autoStatus = "Upcoming";
	if (eventDateStr) {
		const today = new Date();
		today.setHours(0, 0, 0, 0);
		const eventDate = new Date(eventDateStr);
		eventDate.setHours(0, 0, 0, 0);

		if (eventDate.getTime() === today.getTime()) {
			autoStatus = "Live";
		} else if (eventDate.getTime() < today.getTime()) {
			autoStatus = "Passed";
			if (!isUpdate) {
				showToast("Invalid Date", "You cannot add an event with a past date.", "error");
				return;
			}
		}
	}

	const isPaid = document.getElementById('event-paid-input').checked;

	const data = {
		title: document.getElementById('event-title-input').value,
		price: isPaid ? document.getElementById('event-price-input').value : "0",
		paidEvent: isPaid,
		location: document.getElementById('event-loc-input').value,
		description: document.getElementById('event-desc-input').value,
		maxSeats: document.getElementById('event-seats-input').value,
		eventDate: eventDateStr,
		startTime: document.getElementById('event-start-time-input').value,
		endTime: document.getElementById('event-end-time-input').value,
		banner_image: bannerBase64,
		status: autoStatus,
		category: { categId: parseInt(document.getElementById('event-cat-select').value) }
	};
	if (isUpdate) data.eventId = parseInt(eventId);

	const submitBtn = form.querySelector('button[type="submit"]');
	window.setButtonLoading(submitBtn);

	const url = isUpdate ? `/api/events/update/${eventId}` : "/api/events/add";
	const method = isUpdate ? "PUT" : "POST";

	fetch(url, {
		method: method,
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(data)
	})
		.then(async res => {
			if (res.ok) {
				closeModal('event-modal');
				form.reset();
				loadStats();
				loadEvents();
				showToast(isUpdate ? "Event Updated" : "Event Published", isUpdate ? "Changes to the event have been saved successfully." : "Your new event is now live and open for bookings!", "success");
			} else {
				const errorText = await res.text();
				showToast("Error", "Operation failed: " + (errorText || res.status), "error");
			}
		})
		.catch(err => {
			console.error("Error:", err);
			showToast("Error", "An unexpected error occurred.", "error");
		})

		.finally(() => {
			window.resetButtonLoading(submitBtn);
		});
}

document.addEventListener("DOMContentLoaded", () => {
	loadStats();
	loadCategories();
	loadEvents();
	refreshIcons();

	// Sidebar Sub-menu Toggle
	const mgmtParent = document.getElementById('mgmt-parent');
	const mgmtSubmenu = document.getElementById('mgmt-submenu');
	if (mgmtParent && mgmtSubmenu) {
		mgmtParent.addEventListener('click', (e) => {
			// If clicking the chevron or preventing bubble, toggle sub-menu
			if (e.target.closest('.chevron-icon')) {
				e.preventDefault();
				mgmtSubmenu.classList.toggle('active');
				mgmtParent.classList.toggle('open');
			}
		});
	}

	// Handle Action Query Param (from other pages)
	const urlParams = new URLSearchParams(window.location.search);
	const action = urlParams.get('action');
	if (action === 'add-event') {
		location.href = "/api/create-event";
	} else if (action === 'add-category') {
		location.href = "/api/create-category";
	} else if (action === 'update-events') {
		setTimeout(() => {
			document.getElementById('eve-toggle')?.click();
			showDetailedStats('events');
		}, 500);
	} else if (action === 'update-categories') {
		setTimeout(() => {
			document.getElementById('cat-toggle')?.click();
			showDetailedStats('categories');
		}, 500);
	}

	// 3rd Level Nesting toggle
	const setupNestedToggle = (toggleId, menuId) => {
		const toggle = document.getElementById(toggleId);
		const menu = document.getElementById(menuId);
		if (toggle && menu) {
			toggle.addEventListener('click', (e) => {
				// Prevent only if clicking the toggle itself, not a child link
				if (e.target.closest('a')) return;

				e.preventDefault();
				const isOpen = menu.classList.contains('active');
				if (isOpen) {
					menu.classList.remove('active');
					toggle.classList.remove('open');
				} else {
					menu.classList.add('active');
					toggle.classList.add('open');
				}
			});
		}
	};

	setupNestedToggle('cat-toggle', 'cat-nested-menu');
	setupNestedToggle('eve-toggle', 'eve-nested-menu');

	/**
	 * Automatically highlights the active sidebar link and expands its parents
	 * DISABLED to ensure navbar consistency across all pages
	 */
	function highlightActiveNavLink() {
		// Completely disabled to keep sidebar identical on all pages
		// Users can manually expand/collapse menus using chevron icons
	}

	// highlightActiveNavLink(); // DISABLED

	document.getElementById('add-category-btn')?.addEventListener('click', () => {
		document.getElementById('category-modal-title').innerText = "Add New Category";
		document.getElementById('category-submit-btn').innerText = "Create Category";
		document.getElementById('cat-id-input').value = "";
		document.getElementById('add-category-form').reset();
		hideCategoryImagePreview();
		openModal('category-modal');
	});

	// Paid Event Toggle Logic
	const paidCheckbox = document.getElementById('event-paid-input');
	const priceInput = document.getElementById('event-price-input');
	const priceContainer = document.getElementById('price-container');

	if (paidCheckbox && priceInput) {
		const updatePriceState = () => {
			if (paidCheckbox.checked) {
				priceInput.disabled = false;
				priceInput.required = true;
				priceInput.placeholder = "e.g. 50";
				if (priceInput.value === "0") priceInput.value = "";
			} else {
				priceInput.disabled = true;
				priceInput.required = false;
				priceInput.value = "0";
				priceInput.placeholder = "Free";
			}
		};

		paidCheckbox.addEventListener('change', updatePriceState);
	}

	document.getElementById('add-event-btn')?.addEventListener('click', () => {
		document.getElementById('event-modal-title').innerText = "Add New Event";
		document.getElementById('event-submit-btn').innerText = "Publish Event";
		document.getElementById('event-id-input').value = "";
		const form = document.getElementById('add-event-form');
		form.reset();

		// Reset Paid State
		if (paidCheckbox) {
			paidCheckbox.checked = true;
			paidCheckbox.dispatchEvent(new Event('change'));
		}

		hideEventBannerPreview();
		openModal('event-modal');
	});

	document.getElementById('add-category-form')?.addEventListener('submit', handleCategorySubmit);
	document.getElementById('add-event-form')?.addEventListener('submit', handleEventSubmit);
	document.getElementById('quick-register-form')?.addEventListener('submit', handleQuickRegister);

	document.querySelectorAll('.close-modal').forEach(btn => {
		btn.addEventListener('click', () => {
			const modal = btn.closest('.modal-overlay');
			if (modal) modal.classList.remove('active');
		});
	});

	document.getElementById('confirm-cancel-btn')?.addEventListener('click', () => closeModal('confirm-modal'));
	document.getElementById('confirm-ok-btn')?.addEventListener('click', () => {
		if (confirmCallback) confirmCallback();
		closeModal('confirm-modal');
	});

	// Live Preview for Category Icon in Modal
	document.getElementById('cat-icon-input')?.addEventListener('change', function (e) {
		const file = e.target.files[0];
		if (file) {
			const reader = new FileReader();
			reader.onload = (event) => {
				const base64 = event.target.result.split(',')[1];
				showCategoryImagePreview(base64);
			};
			reader.readAsDataURL(file);
		}
	});

	// Live Preview for Event Banner in Modal
	document.getElementById('event-banner-input')?.addEventListener('change', function (e) {
		const file = e.target.files[0];
		if (file) {
			const reader = new FileReader();
			reader.onload = (event) => {
				const base64 = event.target.result.split(',')[1];
				showEventBannerPreview(base64);
			};
			reader.readAsDataURL(file);
		}
	});

	const container = document.querySelector("#category-container");
	document.querySelector("#scroll-left")?.addEventListener("click", () => container.scrollBy({ left: -300, behavior: 'smooth' }));
	document.querySelector("#scroll-right")?.addEventListener("click", () => container.scrollBy({ left: 300, behavior: 'smooth' }));

	const dateInput = document.getElementById('event-date-input');
	if (dateInput) {
		const today = new Date().toISOString().split('T')[0];
		dateInput.setAttribute('min', today);
	}

	// Stats Click Handlers
	document.getElementById('stat-card-categories')?.addEventListener('click', () => showDetailedStats('categories'));
	document.getElementById('stat-card-events')?.addEventListener('click', () => showDetailedStats('events'));
	document.getElementById('stat-card-bookings')?.addEventListener('click', () => showDetailedStats('bookings'));


});

/**
 * Shows detailed statistics in a modal
 */
async function showDetailedStats(type) {
	const modal = document.getElementById('detailed-info-modal');
	const title = document.getElementById('info-modal-title');
	const subtitle = document.getElementById('info-modal-subtitle');
	const body = document.getElementById('info-modal-body');

	if (!modal || !title || !subtitle || !body) {
		// Fallback: Redirect to home page with action parameter if modal is missing
		window.location.href = `/?action=update-${type}`;
		return;
	}

	body.innerHTML = '<div style="text-align: center; padding: 2rem;"><i data-lucide="loader-2" class="animate-spin" size="32"></i><p>Loading details...</p></div>';
	openModal('detailed-info-modal');
	refreshIcons();

	try {
		if (type === 'categories') {
			title.innerText = "Available Categories";
			subtitle.innerText = "List of all active event categories";

			const res = await fetch('/api/categories/get');
			const categories = await res.json();

			if (categories.length === 0) {
				body.innerHTML = `
					<div style="text-align: center; padding: 4rem 2rem; color: var(--text-muted);">
						<i data-lucide="folder-x" size="48" style="margin-bottom: 1rem; opacity: 0.5; color: var(--primary);"></i>
						<p style="font-size: 1.1rem; font-weight: 500;">No categories found</p>
						<p style="font-size: 0.85rem; margin-top: 0.5rem;">Start by adding your first event category to see statistics.</p>
					</div>
				`;
				refreshIcons();
				return;
			}

			body.innerHTML = categories.map(cat => `
				<div class="stat-item-row">
					<div class="stat-item-info">
						<div class="stat-item-icon" style="background: ${cat.catColor || 'var(--primary)'}">
							<i data-lucide="layers" size="18"></i>
						</div>
						<div>
							<div style="font-weight: 700; color: var(--text-main);">${cat.catName}</div>
							<div style="font-size: 0.8rem; color: var(--text-muted);">${cat.description || 'No description'}</div>
						</div>
					</div>
					<div class="info-badge" style="background: #e0f2fe; color: #0369a1;">
						${cat.events ? cat.events.length : 0} Events
					</div>
				</div>
			`).join('');
		}
		else if (type === 'events') {
			title.innerText = "Active Events";
			subtitle.innerText = "All upcoming and live events classified by category";

			const res = await fetch('/api/events/getall');
			const allEvents = await res.json();

			if (allEvents.length === 0) {
				body.innerHTML = `
					<div style="text-align: center; padding: 4rem 2rem; color: var(--text-muted);">
						<i data-lucide="calendar-off" size="48" style="margin-bottom: 1rem; opacity: 0.5; color: var(--primary);"></i>
						<p style="font-size: 1.1rem; font-weight: 500;">No active events found</p>
						<p style="font-size: 0.85rem; margin-top: 0.5rem;">Create your first event to see detailed event statistics here.</p>
					</div>
				`;
				refreshIcons();
				return;
			}

			body.innerHTML = allEvents.map(eve => `
				<div class="stat-item-row">
					<div class="stat-item-info">
						<div class="stat-item-icon" style="background: #eef2ff; color: var(--primary);">
							<i data-lucide="calendar" size="18"></i>
						</div>
						<div>
							<div style="font-weight: 700; color: var(--text-main);">${eve.title}</div>
							<div style="font-size: 0.8rem; color: var(--text-muted);">
								Category: <span style="color: var(--primary); font-weight: 600;">${eve.category ? eve.category.catName : 'General'}</span> <span class="mobile-hidden">|</span> <span class="mobile-break">Date: ${eve.eventDate || 'TBD'}</span>
							</div>
						</div>
					</div>
					<div class="info-badge" style="background: ${eve.status === 'Live' ? '#dcfce7' : '#fee2e2'}; color: ${eve.status === 'Live' ? '#166534' : '#991b1b'};">
						${eve.status}
					</div>
				</div>
			`).join('');
		}
		else if (type === 'bookings') {
			title.innerText = "Event Bookings & Revenue";
			subtitle.innerText = "Hierarchical view: Category > Event > Customer Tickets";

			const [catRes, bookRes] = await Promise.all([
				fetch('/api/categories/get'),
				fetchWithAuth('/api/bookings/admin/all')
			]);
			const categories = await catRes.json();
			const bookings = await bookRes.json();

			// Group bookings by eventId
			const bookingsByEvent = bookings.reduce((acc, b) => {
				const eid = b.event.eventId;
				if (!acc[eid]) acc[eid] = [];
				acc[eid].push(b);
				return acc;
			}, {});

			let html = '';
			categories.forEach(cat => {
				const catEvents = cat.events || [];
				if (catEvents.length > 0) {
					html += `
						<div class="info-group">
							<div class="info-group-title">
								<div style="width: 10px; height: 10px; border-radius: 50%; background: ${cat.catColor || 'var(--primary)'}"></div>
								${cat.catName}
							</div>
							${catEvents.map(eve => {
						const eveBookings = bookingsByEvent[eve.eventId] || [];
						const totalRevenue = eveBookings.reduce((sum, b) => sum + b.totalAmount, 0);
						return `
									<div class="info-sub-item" style="border-left-color: ${cat.catColor || 'var(--primary)'}">
										<div class="info-sub-header">
											<div>
												<div class="info-sub-title">${eve.title}</div>
												<div style="font-size: 0.75rem; color: var(--text-muted);">Status: ${eve.status} | Revenue: ₹${totalRevenue.toFixed(2)}</div>
											</div>
											<div class="info-badge" style="background: #f1f5f9; color: #475569;">
												${eveBookings.length} Bookings
											</div>
										</div>
										
										${eveBookings.length > 0 ? `
											<div class="booking-ticket-list">
												<div class="booking-row booking-header">
													<span>Customer</span>
													<span>Mobile</span>
													<span>Org</span>
													<span>Type</span>
													<span>Status</span>
													<span>Total</span>
													<span>Action</span>
												</div>
												${eveBookings.map(b => `
													<div class="booking-row">
														<span data-label="Customer" style="font-weight: 700; color: var(--text-main);">${b.customerName}</span>
														<span data-label="Mobile">${b.mobileNumber || 'N/A'}</span>
														<span data-label="Organization" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${b.organization}">${b.organization || 'External'}</span>
														<span data-label="User Type"><span class="info-badge" size="small" style="background: ${b.userType === 'Internal' ? '#eef2ff' : '#f8fafc'}; color: ${b.userType === 'Internal' ? 'var(--primary)' : '#64748b'};">${b.userType || 'N/A'}</span></span>
														<span data-label="Status"><span class="info-badge" style="font-size: 0.65rem; background: ${b.status === 'Confirmed' ? '#dcfce7' : '#fef9c3'}">${b.status}</span></span>
														<span data-label="Total Amount" style="font-weight: 800; color: var(--primary);">₹${b.totalAmount.toFixed(2)}</span>
														<span data-label="Action" class="booking-action-cell">
															<button class="small-action-btn" onclick="viewTicket(this, ${b.bookingId})" title="View Ticket Card">
																<i data-lucide="external-link" size="14"></i>
															</button>
														</span>
													</div>
												`).join('')}
											</div>
										` : '<div style="font-size: 0.8rem; color: var(--text-muted); font-style: italic; margin-top: 10px;">No bookings yet for this event.</div>'}
									</div>
								`;
					}).join('')}
						</div>
					`;
				}
			});

			if (!html) {
				html = `
					<div style="text-align: center; padding: 4rem 2rem; color: var(--text-muted);">
						<i data-lucide="ticket" size="48" style="margin-bottom: 1rem; opacity: 0.5; color: var(--primary);"></i>
						<p style="font-size: 1.1rem; font-weight: 500;">No bookings available</p>
						<p style="font-size: 0.85rem; margin-top: 0.5rem;">Total revenue and customer lists will appear once tickets are booked.</p>
					</div>
				`;
			}
			body.innerHTML = html;
		}

		refreshIcons();
	} catch (err) {
		console.error("Error loading detailed stats:", err);
		body.innerHTML = '<div style="text-align: center; color: var(--accent); padding: 2rem;">Failed to load data. Please check connection.</div>';
	}
}

/**
 * Global Booking Store for Preview
 */
let allBookingsCache = {};

async function viewTicket(btn, bookingId) {
	// Handle old signature viewTicket(bookingId) just in case
	if (typeof btn === 'number' || typeof btn === 'string') {
		bookingId = btn;
		btn = null;
	}

	if (btn) window.setIconButtonLoading(btn);

	let b = allBookingsCache[bookingId];

	if (!b) {
		try {
			const res = await fetchWithAuth(`/api/bookings/admin/all`);
			const all = await res.json();
			all.forEach(item => allBookingsCache[item.bookingId] = item);
			b = allBookingsCache[bookingId];
		} catch (e) { console.error(e); }
	}

	if (btn) window.resetIconButtonLoading(btn);


	if (!b) {
		showToast("Error", "Ticket data not found.", "error");
		return;
	}

	const event = b.event || {};
	const timeRange = event.startTime ? (event.endTime ? `${formatTime12Hour(event.startTime)} - ${formatTime12Hour(event.endTime)}` : formatTime12Hour(event.startTime)) : 'TBD';

	const ticketAmount = (typeof b.totalAmount === 'number' && b.totalAmount > 0)
		? `₹${b.totalAmount.toFixed(2)}`
		: '<span style="color: var(--secondary);">FREE</span>';

	const content = `
		<div class="official-ticket" style="margin-bottom: 0;">
			<div class="official-ticket-main">
				<div class="ticket-org-badge">Official Pass</div>
				<h2 class="ticket-event-name">${event.title || 'Event'}</h2>
				
				<div class="ticket-info-grid">
					<div class="ticket-info-item">
						<label>Date</label>
						<span><i data-lucide="calendar" size="14"></i> ${event.eventDate || 'TBD'}</span>
					</div>
					<div class="ticket-info-item">
						<label>Time</label>
						<span><i data-lucide="clock" size="14"></i> ${timeRange}</span>
					</div>
					<div class="ticket-info-item" style="grid-column: 1 / -1;">
						<label>Venue</label>
						<span><i data-lucide="map-pin" size="14"></i> ${event.location || 'Online/TBD'}</span>
					</div>
				</div>

				<div class="ticket-attendee-details">
					<div class="ticket-info-item">
						<label>Attendee</label>
						<span style="font-size: 1rem; color: var(--primary);">${b.customerName || 'Guest'}</span>
					</div>
					<div class="ticket-info-item">
						<label>Attendee Type</label>
						<span class="status-tag ${b.userType ? b.userType.toLowerCase() : 'external'}" style="font-size: 0.65rem; padding: 2px 8px;">
							<i data-lucide="user-check" size="12"></i> ${b.userType || 'External'}
						</span>
					</div>
					<div class="ticket-info-item">
						<label>Organization</label>
						<span>${b.organization || '-'}</span>
					</div>
					<div class="ticket-info-item">
						<label>Price</label>
						<span>${ticketAmount}</span>
					</div>
				</div>
			</div>

			<div class="official-ticket-stub">
				<div class="ticket-qr-box">
					<img src="https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=${encodeURIComponent(window.location.origin + '/api/bookings/validate/' + b.bookingId)}" alt="QR" style="width: 100%; height: 100%;">
				</div>
				<div class="ticket-info-item">
					<label>Order ID</label>
					<div class="ticket-order-tag">#ORD-${b.bookingId}</div>
				</div>
				<div class="status-tag ${b.status ? b.status.toLowerCase() : 'pending'}" style="margin-top: 10px; font-size: 0.65rem;">
					${b.status || 'Pending'}
				</div>
			</div>
		</div>
	`;

	document.getElementById('ticket-preview-content').innerHTML = content;
	openModal('ticket-preview-modal');
	refreshIcons();
}

/**
 * Show Category Image Preview in Modal
 */
function showCategoryImagePreview(catIconBase64) {
	const previewWrapper = document.getElementById('cat-icon-preview-wrapper');
	const previewImg = document.getElementById('cat-icon-preview');

	if (catIconBase64 && previewWrapper && previewImg) {
		previewImg.src = 'data:image/png;base64,' + catIconBase64;
		previewWrapper.style.display = 'block';
	} else if (previewWrapper) {
		previewWrapper.style.display = 'none';
	}
}

/**
 * Show Event Banner Preview in Modal
 */
function showEventBannerPreview(bannerBase64) {
	const previewWrapper = document.getElementById('event-banner-preview-wrapper');
	const previewImg = document.getElementById('event-banner-preview');

	if (bannerBase64 && previewWrapper && previewImg) {
		previewImg.src = 'data:image/jpeg;base64,' + bannerBase64;
		previewWrapper.style.display = 'block';
	} else if (previewWrapper) {
		previewWrapper.style.display = 'none';
	}
}

/**
 * Hide Category Image Preview
 */
function hideCategoryImagePreview() {
	const previewWrapper = document.getElementById('cat-icon-preview-wrapper');
	if (previewWrapper) {
		previewWrapper.style.display = 'none';
	}
}

/**
 * Hide Event Banner Preview
 */
function hideEventBannerPreview() {
	const previewWrapper = document.getElementById('event-banner-preview-wrapper');
	if (previewWrapper) {
		previewWrapper.style.display = 'none';
	}
}
/**
 * Initialize
 */
document.addEventListener("DOMContentLoaded", () => {
	console.log('Initializing Dashboard Scripts...');
	refreshIcons();
	loadCategories();
	console.log('Called loadCategories');
	loadEvents();
	console.log('Called loadEvents');

	if (isAuthenticated()) {
		loadStats();
		if (typeof updateCartCount === 'function') {
			updateCartCount();
		}
	}

	// Sidebar active state
	const currentPath = window.location.pathname;
	document.querySelectorAll('.app-sidebar nav a').forEach(link => {
		if (link.getAttribute('href') === currentPath) {
			link.classList.add('active');
		} else {
			link.classList.remove('active');
		}
	});

	// File Input Previews
	const catIconInput = document.getElementById('cat-icon-input');
	if (catIconInput) {
		catIconInput.addEventListener('change', function (e) {
			const file = e.target.files[0];
			if (file) {
				const reader = new FileReader();
				reader.onload = function (e) {
					showCategoryImagePreview(e.target.result.split(',')[1]);
				}
				reader.readAsDataURL(file);
			}
		});
	}

	const eventBannerInput = document.getElementById('event-banner-input');
	if (eventBannerInput) {
		eventBannerInput.addEventListener('change', function (e) {
			const file = e.target.files[0];
			if (file) {
				const reader = new FileReader();
				reader.onload = function (e) {
					showEventBannerPreview(e.target.result.split(',')[1]);
				}
				reader.readAsDataURL(file);
			}
		});
	}

	// Scroll Buttons for Categories
	const scrollContainer = document.getElementById('category-container');
	const leftBtn = document.getElementById('scroll-left');
	const rightBtn = document.getElementById('scroll-right');

	if (scrollContainer && leftBtn && rightBtn) {
		leftBtn.onclick = () => {
			scrollContainer.scrollBy({ left: -300, behavior: 'smooth' });
		};
		rightBtn.onclick = () => {
			scrollContainer.scrollBy({ left: 300, behavior: 'smooth' });
		};
	}

	/**
	 * GLOBAL SEARCH CONTROLLER
	 */
	function initGlobalSearch() {
		const eventSearchInput = document.getElementById('event-search');
		if (!eventSearchInput) return;

		const dropdown = document.getElementById('search-results-dropdown');
		if (!dropdown) return; // Should exist if header fragment is used

		let cachedEvents = null;
		let cachedCategories = null;
		let cachedBookings = null;

		// Fetch data once on focus if not already fetched
		const fetchData = async () => {
			if (cachedEvents && cachedCategories) return;
			try {
				const promises = [
					fetch('/api/events/getall'),
					fetch('/api/categories/get')
				];

				const token = localStorage.getItem('jwtToken');
				if (token) {
					promises.push(fetchWithAuth('/api/bookings/getall'));
				}

				const results = await Promise.all(promises);

				if (results[0] && results[0].ok) cachedEvents = await results[0].json();
				if (results[1] && results[1].ok) cachedCategories = await results[1].json();
				if (token && results[2] && results[2].ok) {
					cachedBookings = await results[2].json();
				}
			} catch (e) {
				console.error("Failed to fetch search data", e);
			}
		};

		eventSearchInput.addEventListener('focus', fetchData);

		// Remove old listeners if any (though not easily possible without reference)
		// Ensure no other part of the code is attaching input listener to this ID.

		const debouncedSearch = debounce(() => {
			const query = eventSearchInput.value.toLowerCase().trim();
			if (!query) {
				dropdown.style.display = 'none';
				return;
			}

			if (!cachedEvents || !cachedCategories) {
				// Retry fetch if failed or not ready
				fetchData().then(() => debouncedSearch());
				return;
			}

			const matchedEvents = cachedEvents.filter(e =>
				(e.title && e.title.toLowerCase().includes(query)) ||
				(e.description && e.description.toLowerCase().includes(query)) ||
				(e.location && e.location.toLowerCase().includes(query))
			).slice(0, 5); // Limit to 5 results

			const matchedCategories = cachedCategories.filter(c =>
				(c.catName && c.catName.toLowerCase().includes(query))
			).slice(0, 3); // Limit to 3 results

			const matchedBookings = cachedBookings ? cachedBookings.filter(b => {
				const eventTitle = (b.event && b.event.title) || '';
				const orderId = `#ORD-${b.bookingId}`;
				return eventTitle.toLowerCase().includes(query) ||
					orderId.toLowerCase().includes(query) ||
					(b.customerName && b.customerName.toLowerCase().includes(query));
			}).slice(0, 3) : [];

			renderDropdown(matchedEvents, matchedCategories, matchedBookings, query);
		}, 300);

		eventSearchInput.addEventListener('input', debouncedSearch);

		// Close dropdown when clicking outside
		document.addEventListener('click', (e) => {
			if (!eventSearchInput.contains(e.target) && !dropdown.contains(e.target)) {
				dropdown.style.display = 'none';
			}
		});
	}

	function renderDropdown(events, categories, bookings, query) {
		const dropdown = document.getElementById('search-results-dropdown');
		if (!dropdown) return;

		let html = '';

		if (categories.length > 0) {
			html += `<div class="search-group-title">Categories</div>`;
			categories.forEach(cat => {
				const iconHtml = cat.catIconBase64
					? `<img src="data:image/jpeg;base64,${cat.catIconBase64}" alt="icon">`
					: `<i class="fa-solid fa-folder"></i>`;

				html += `
                    <div class="search-result-item" onclick="handleCategoryClick(${cat.categId}, '${cat.catName.replace(/'/g, "\\'")}')">
                        <div class="result-icon">${iconHtml}</div>
                        <div class="result-info">
                            <div class="result-title">${highlightMatch(cat.catName, query)}</div>
                            <div class="result-subtitle">View all events in this category</div>
                        </div>
                        <i class="fa-solid fa-chevron-right" style="color: var(--text-muted); font-size: 0.8rem;"></i>
                    </div>
                `;
			});
		}

		if (events.length > 0) {
			html += `<div class="search-group-title">Events</div>`;
			events.forEach(event => {
				const imgHtml = event.bannerBase64
					? `<img src="data:image/jpeg;base64,${event.bannerBase64}" alt="banner">`
					: `<div class="result-icon" style="background:${getRandomColor(event.title)}; color:white;"><span style="font-weight:700;">${event.title.charAt(0)}</span></div>`;

				// Adjust icon if banner is missing to be consistent
				const iconDisplay = event.bannerBase64
					? `<div class="result-icon"><img src="data:image/jpeg;base64,${event.bannerBase64}" style="width:100%; height:100%; object-fit:cover;"></div>`
					: `<div class="result-icon"><i class="fa-solid fa-calendar-day"></i></div>`;


				html += `
                    <div class="search-result-item" onclick="window.location.href='/event-details?id=${event.eventId}'">
                        ${iconDisplay}
                        <div class="result-info">
                            <div class="result-title">${highlightMatch(event.title, query)}</div>
                            <div class="result-subtitle">${event.eventDate || 'Date TBD'} • ${event.location || 'Location TBD'}</div>
                        </div>
                    </div>
                `;
			});
		}

		if (bookings && bookings.length > 0) {
			html += `<div class="search-group-title">My Tickets</div>`;
			bookings.forEach(b => {
				const statusClass = (b.status || 'Pending').toLowerCase();
				html += `
                    <div class="search-result-item" onclick="window.location.href='/tickets'">
                        <div class="result-icon"><i class="fa-solid fa-ticket"></i></div>
                        <div class="result-info">
                            <div class="result-title">${highlightMatch(b.event ? b.event.title : 'Ticket', query)}</div>
                            <div class="result-subtitle">#ORD-${b.bookingId} • ${b.status}</div>
                        </div>
                        <span class="status-tag ${statusClass}" style="transform: scale(0.8);">${b.status}</span>
                    </div>
                `;
			});
		}

		if (events.length === 0 && categories.length === 0 && (!bookings || bookings.length === 0)) {
			html = `
                <div class="no-results-item" style="padding: 2rem; text-align: center; opacity: 0.7;">
                    <i class="fa-solid fa-magnifying-glass-minus" style="font-size: 1.5rem; margin-bottom: 0.75rem; color: var(--text-muted);"></i>
                    <p style="margin: 0; font-size: 0.9rem; font-weight: 500;">No results found for "${query}"</p>
                </div>
            `;
		}

		dropdown.innerHTML = html;
		dropdown.style.display = 'block';
		if (window.lucide) window.lucide.createIcons();
	}

	// Helper functions for search
	function debounce(func, wait) {
		let timeout;
		return function executedFunction(...args) {
			const later = () => {
				clearTimeout(timeout);
				func(...args);
			};
			clearTimeout(timeout);
			timeout = setTimeout(later, wait);
		};
	}

	function highlightMatch(text, query) {
		if (!query) return text;
		const regex = new RegExp(`(${query})`, 'gi');
		return text.replace(regex, '<span style="background: rgba(255, 255, 0, 0.3); color: inherit;">$1</span>');
	}

	function getRandomColor(str) {
		const colors = ['#ef4444', '#f59e0b', '#10b981', '#3b82f6', '#6366f1', '#8b5cf6', '#ec4899'];
		let hash = 0;
		for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
		return colors[Math.abs(hash) % colors.length];
	}

	// Make handleCategoryClick global so onclick works
	window.handleCategoryClick = function (id, name) {
		// If we are on index page, open modal directly
		if (typeof openCategoryEvents === 'function' && (window.location.pathname === '/' || window.location.pathname === '/index.html' || document.getElementById('category-events-modal'))) {
			openCategoryEvents(id, name);
			if (typeof openModal === 'function') openModal('category-events-modal');
		} else {
			// Redirect to home with open category param
			// Assuming home page handles 'action' or similar, but simpler to just implement modal opening logic or redirect
			// Since User requested "opens category popup", let's redirect to home and trigger it if not on home
			window.location.href = `/?openCategory=${id}&catName=${encodeURIComponent(name)}`;
		}
	};

	// Check for openCategory param on load
	const params = new URLSearchParams(window.location.search);
	const openCatId = params.get('openCategory');
	const openCatName = params.get('catName');
	if (openCatId && openCatName) {
		// Wait for script to load fully/dom ready
		setTimeout(() => {
			if (typeof openCategoryEvents === 'function' && document.getElementById('category-events-modal')) {
				openCategoryEvents(parseInt(openCatId), openCatName);
				if (typeof openModal === 'function') openModal('category-events-modal');

				// Clean URL
				const newUrl = window.location.pathname;
				window.history.replaceState({}, document.title, newUrl);
			}
		}, 1000);
	}

	initGlobalSearch();
	loadHeaderProfile();
});

/**
 * Loads the user's profile image into the header across all pages
 */
async function loadHeaderProfile() {
	const img = document.getElementById('header-profile-img');
	const icon = document.getElementById('header-profile-icon');

	if (!img || !icon) return;

	const token = localStorage.getItem('jwtToken');
	if (!token) return; // Guest user

	try {
		const response = await fetch('/api/profile/me', {
			headers: { 'Authorization': `Bearer ${token}` }
		});

		if (response.ok) {
			const user = await response.json();
			if (user.profileImage) {
				img.src = `data:image/jpeg;base64,${user.profileImage}`;
				img.style.display = 'block';
				icon.style.display = 'none';
			} else {
				// Keep default icon
				img.style.display = 'none';
				icon.style.display = 'block';
			}
		}
	} catch (e) {
		// Silent fail
	}
}
