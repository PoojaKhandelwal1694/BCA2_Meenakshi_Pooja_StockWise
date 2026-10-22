
const API_URL = "http://127.0.0.1:8080/api/auth";

// Send request to Spring Boot backend
async function sendAuthRequest(endpoint, data) {
    const response = await fetch(`${API_URL}/${endpoint}`, {
        method: "POST",
        credentials: "include",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(data)
    });

    const result = await response.json();

    if (!response.ok) {
        throw new Error(result.message || "Request failed.");
    }

    return result;
}

// SIGNUP
const signupForm = document.getElementById("signupForm");

if (signupForm) {
    signupForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const message = document.getElementById("message");

        const fullName =
            document.getElementById("fullName").value.trim();

        const email =
            document.getElementById("email").value.trim();

        const password =
            document.getElementById("password").value;

        const confirmPassword =
            document.getElementById("confirmPassword").value;

        if (password.length < 8) {
            message.textContent =
                "Password must contain at least 8 characters.";
            return;
        }

        if (password !== confirmPassword) {
            message.textContent = "Passwords do not match.";
            return;
        }

        const button = signupForm.querySelector(
            'button[type="submit"]'
        );

        button.disabled = true;
        message.textContent = "Creating your account...";

        try {
            const result = await sendAuthRequest("signup", {
                fullName: fullName,
                email: email,
                password: password
            });

            message.textContent = result.message;

            setTimeout(function () {
                window.location.href = "login.html";
            }, 1000);

        } catch (error) {
            message.textContent = error.message;

        } finally {
            button.disabled = false;
        }
    });
}

// LOGIN
const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const message = document.getElementById("message");

        const email =
            document.getElementById("email").value.trim();

        const password =
            document.getElementById("password").value;

        const button = loginForm.querySelector(
            'button[type="submit"]'
        );

        button.disabled = true;
        message.textContent = "Logging in...";

        try {
            const result = await sendAuthRequest("login", {
                email: email,
                password: password
            });

            message.textContent = result.message;

            setTimeout(function () {
                window.location.href = "dashboard.html";
            }, 500);

        } catch (error) {
            message.textContent = error.message;

        } finally {
            button.disabled = false;
        }
    });
}
