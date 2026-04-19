# ⚡ CLI Caching Proxy Server

A robust Command Line Interface (CLI) tool that functions as a caching proxy server. It optimizes network performance by forwarding requests to an origin server and caching the responses in **Redis** for instantaneous subsequent retrieval.

## 🌟 Key Features

- **Dynamic Configuration:** Easily set custom ports and origin URLs via CLI arguments.
- **High Performance:** Utilizes **Redis** for efficient response caching, significantly reducing latency.
- **Transparency:** Adds custom `X-Cache` headers (**HIT/MISS**) to help developers track cache performance.
- **Cache Management:** Built-in commands to clear the cache instantly.
- **Native Implementation:** Developed using **Java 11+ HttpClient** and native networking libraries for a lightweight footprint.

## 🛠 Tech Stack

- **Language:** Java 17
- **CLI Framework:** Picocli
- **Memory Store:** Redis
- **Build Tool:** Maven

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Redis (running on localhost:6379 via Docker)

### Installation & Execution

1. **Clone the repo:**
   ```bash
   git clone https://github.com/isilulgerr/caching-proxy-cli.git
   cd caching-proxy-cli
2. **Build the project:**
   ```bash
   mvn clean compile
   ```

3. **Start the proxy:**
   ```bash
   mvn exec:java "-Dexec.mainClass=com.proxy.CachingProxyCLI" "-Dexec.args=--port 3000 --origin https://dummyjson.com"
   ```

4. **Clear the cache:**
   ```bash
   mvn exec:java "-Dexec.mainClass=com.proxy.CachingProxyCLI" "-Dexec.args=--clear-cache"
   ```

## 📊 How It Works

- **Initial Request (MISS):** The proxy fetches data from the origin server, saves it to Redis, and returns it with `X-Cache: MISS`.
- **Subsequent Requests (HIT):** The proxy serves data directly from Redis, achieving near-zero latency with `X-Cache: HIT`.

---
*Developed by Işıl Ülger - Computer Engineer*