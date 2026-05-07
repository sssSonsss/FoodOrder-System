function cancelOrder(orderId) {
    if (!confirm("Bạn có chắc muốn hủy đơn này?")) return;
    const body = new URLSearchParams({ action: "cancel", orderId: String(orderId) });
    fetch("OrderServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: body.toString()
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            alert(data.error);
            return;
        }
        alert("Hủy đơn thành công.");
        window.location.reload();
    })
    .catch(() => alert("Có lỗi xảy ra khi hủy đơn."));
}

function reorder(orderId) {
    const body = new URLSearchParams({ action: "reorder", orderId: String(orderId) });
    fetch("OrderServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: body.toString()
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            alert(data.error);
            return;
        }
        alert("Đã thêm món từ đơn cũ vào giỏ hàng.");
    })
    .catch(() => alert("Có lỗi xảy ra khi mua lại."));
}

function submitReview(event, orderId, foodId) {
    event.preventDefault();
    const form = event.target;
    const rating = form.querySelector('select[name="rating"]').value;
    const comment = form.querySelector('textarea[name="comment"]').value;

    const body = new URLSearchParams({
        action: "review",
        orderId: String(orderId),
        foodId: String(foodId),
        rating: String(rating),
        comment: comment
    });

    fetch("OrderServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: body.toString()
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            alert(data.error);
            return;
        }
        alert("Cảm ơn bạn đã đánh giá món ăn.");
        form.reset();
    })
    .catch(() => alert("Không thể gửi đánh giá lúc này."));
}
