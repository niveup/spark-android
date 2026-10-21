import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { clientIp } from "../src/lib/client-ip";

function request(headers: Record<string, string>): Request {
  return new Request("https://spark.test/api/mobile/v1/auth/login", { headers });
}

describe("clientIp", () => {
  it("prefers Vercel's platform header over a client-supplied XFF", () => {
    assert.equal(
      clientIp(
        request({
          "x-vercel-forwarded-for": "203.0.113.7",
          "x-forwarded-for": "198.51.100.99, 203.0.113.7",
          "x-real-ip": "192.0.2.44",
        }),
      ),
      "203.0.113.7",
    );
  });

  it("uses x-real-ip when the Vercel header is absent", () => {
    assert.equal(clientIp(request({ "x-real-ip": "192.0.2.44" })), "192.0.2.44");
  });

  it("uses the last XFF hop, not the spoofable first value", () => {
    assert.equal(
      clientIp(request({ "x-forwarded-for": "1.2.3.4, 10.0.0.1, 203.0.113.7" })),
      "203.0.113.7",
    );
  });

  it("uses a single shared bucket when no IP header is present", () => {
    assert.equal(clientIp(request({})), "unknown-ip");
  });
});
