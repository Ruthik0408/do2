const form = document.querySelector("#prediction-form");
const body = document.querySelector("#results-body");
const message = document.querySelector("#message");
const submitButton = form.querySelector("button");

const metricIds = {
  rows_returned: "rows-returned",
  matches: "matches",
  mismatches: "mismatches",
  predicted_non_valid: "predicted-non-valid",
};

function setMessage(text, isError = false) {
  message.hidden = !text;
  message.textContent = text || "";
  message.classList.toggle("error", isError);
}

function setMetrics(summary = {}) {
  Object.entries(metricIds).forEach(([key, id]) => {
    document.querySelector(`#${id}`).textContent = Number(summary[key] || 0).toLocaleString();
  });
}

function value(row, key) {
  return row[key] === null || row[key] === undefined || row[key] === "" ? "-" : row[key];
}

function statusPill(status) {
  const isValid = status === "V";
  return `<span class="pill ${isValid ? "valid" : "non-valid"}">${isValid ? "V" : "Not V"}</span>`;
}

function renderRows(rows) {
  if (!rows.length) {
    body.innerHTML = '<tr><td colspan="9" class="empty">No rows found for this date range.</td></tr>';
    return;
  }

  body.innerHTML = rows
    .map((row) => {
      const probability = Number(row.non_valid_probability || 0);
      const match = Boolean(row.matches_actual);
      return `
        <tr>
          <td>${value(row, "id")}</td>
          <td>${value(row, "do2_date")}</td>
          <td>${value(row, "do2_type")}</td>
          <td>${value(row, "actual_status")}</td>
          <td>${statusPill(row.actual_status_group)}</td>
          <td>${statusPill(row.predicted_status_group)}</td>
          <td>${(probability * 100).toFixed(2)}%</td>
          <td>${value(row, "recommended_action")}</td>
          <td><span class="pill ${match ? "match" : "mismatch"}">${match ? "Match" : "Mismatch"}</span></td>
        </tr>
      `;
    })
    .join("");
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  setMessage("");
  submitButton.disabled = true;
  submitButton.textContent = "Running...";

  const params = new URLSearchParams(new FormData(form));
  try {
    const response = await fetch(`/api/predict?${params.toString()}`);
    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.error || "Prediction failed");
    }

    setMetrics(payload.summary);
    renderRows(payload.rows);
    setMessage(`Predicted all ${payload.summary.rows_returned.toLocaleString()} rows in the selected range.`);
  } catch (error) {
    setMessage(error.message, true);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "Run Prediction";
  }
});
