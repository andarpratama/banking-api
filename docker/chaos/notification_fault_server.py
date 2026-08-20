#!/usr/bin/env python3
"""Local notification-vendor fault injector (T-095). GET /health is always 200."""

from __future__ import annotations

import os
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MODE = os.environ.get("FAULT_MODE", "fail").strip().lower()
HANG_SECONDS = int(os.environ.get("FAULT_HANG_SECONDS", "30"))
PORT = int(os.environ.get("FAULT_PORT", "8099"))


class FaultHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def _json(self, code: int, body: str) -> None:
        payload = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self) -> None:
        if self.path.split("?", 1)[0] in ("/health", "/health/"):
            self._json(200, '{"status":"UP","mode":"%s"}' % MODE)
            return
        self._json(404, '{"error":"not_found"}')

    def do_POST(self) -> None:
        length = int(self.headers.get("Content-Length", "0") or "0")
        if length > 0:
            self.rfile.read(length)
        if MODE == "ok":
            self._json(202, '{"status":"accepted"}')
        elif MODE == "hang":
            time.sleep(HANG_SECONDS)
            self._json(504, '{"error":"injected_timeout"}')
        else:
            self._json(500, '{"error":"injected_failure"}')


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), FaultHandler)
    print("notification-fault listening on %s mode=%s" % (PORT, MODE), flush=True)
    server.serve_forever()
