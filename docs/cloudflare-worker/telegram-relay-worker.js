export default {
	async fetch(request, env) {
		const url = new URL(request.url);

		if (request.method !== "POST") {
			return new Response("Method not allowed", { status: 405 });
		}

		const relaySecret = request.headers.get("X-Relay-Secret");
		if (!env.RELAY_SECRET || relaySecret !== env.RELAY_SECRET) {
			return new Response("Unauthorized", { status: 401 });
		}

		const allowedMethods = new Set([
			"getMe",
			"getUpdates",
			"sendMessage",
		]);

		if (!url.pathname.startsWith("/tg/")) {
			return new Response("Not found", { status: 404 });
		}

		const method = url.pathname.replace("/tg/", "");

		if (!allowedMethods.has(method)) {
			return new Response("Method not allowed", { status: 403 });
		}

		if (!env.BOT_TOKEN) {
			return new Response("BOT_TOKEN is not configured", { status: 500 });
		}

		let payload = {};

		try {
			payload = await request.json();
		} catch {
			payload = {};
		}

		const body = new URLSearchParams();

		if (method === "getUpdates") {
			if (payload.offset !== undefined && payload.offset !== null) {
				body.set("offset", String(payload.offset));
			}

			body.set("timeout", "0");
			body.set("allowed_updates", JSON.stringify(["message"]));
		}

		if (method === "sendMessage") {
			if (!payload.chat_id || !payload.text) {
				return new Response("Missing chat_id or text", { status: 400 });
			}

			body.set("chat_id", String(payload.chat_id));
			body.set("text", String(payload.text));
			body.set("parse_mode", "HTML");
			body.set("disable_web_page_preview", "true");
		}

		const telegramUrl = `https://api.telegram.org/bot${env.BOT_TOKEN}/${method}`;

		const tgResponse = await fetch(telegramUrl, {
			method: "POST",
			headers: {
				"Content-Type": "application/x-www-form-urlencoded",
			},
			body,
		});

		const responseText = await tgResponse.text();

		return new Response(responseText, {
			status: tgResponse.status,
			headers: {
				"Content-Type": "application/json; charset=utf-8",
			},
		});
	},
};
