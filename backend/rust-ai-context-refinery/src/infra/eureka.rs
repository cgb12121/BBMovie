use local_ip_address::local_ip;
use serde::Serialize;
use std::cmp::min;
use std::time::Duration;
use tokio::time::sleep;

#[derive(Serialize)]
struct EurekaInstanceWrapper {
    instance: InstanceInfo,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct InstanceInfo {
    instance_id: String,
    host_name: String,
    app: String,
    ip_addr: String,
    status: String,
    port: PortInfo,
    secure_port: PortInfo,
    data_center_info: DataCenterInfo,
    lease_info: LeaseInfo,
    metadata: MetaData,
    home_page_url: String,
    status_page_url: String,
    health_check_url: String,
    vip_address: String,
    secure_vip_address: String,
}

#[derive(Serialize)]
struct PortInfo {
    #[serde(rename = "$")]
    port: u16,
    #[serde(rename = "@enabled")]
    enabled: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct DataCenterInfo {
    #[serde(rename = "@class")]
    class: String,
    name: String,
}

#[derive(Serialize)]
struct LeaseInfo {
    duration_in_secs: u32,
}

#[derive(Serialize)]
struct MetaData {
    instance_id: String,
}

/// Registers with Eureka and maintains a heartbeat in a robust background loop.
/// Handles retries with exponential backoff (5 s -> 5 min) and auto-reconnection.
/// Now a synchronous function that spawns the async task.
pub fn register_and_heartbeat() {
    tokio::spawn(async move {
        let client = reqwest::Client::new();

        // Read Config from Env
        let eureka_url = std::env::var("EUREKA_URL").unwrap_or_else(|_| "http://localhost:8761/eureka".to_string());
        let app_name = std::env::var("APP_NAME").unwrap_or_else(|_| "AI-REFINERY".to_string());
        let port_str = std::env::var("PORT").unwrap_or_else(|_| "8686".to_string());
        let port = port_str.parse::<u16>().unwrap_or(8686);

        // Resolve Instance Info (Robustly)
        let ip = local_ip().unwrap_or_else(|_| "127.0.0.1".parse().unwrap()).to_string();
        let hostname = hostname::get()
            .unwrap_or_else(|_| "localhost".into())
            .to_string_lossy()
            .to_string();
        let instance_id = format!("{}:{}:{}", hostname, app_name, port);

        // Backoff params
        let initial_backoff = Duration::from_secs(5);
        let max_backoff = Duration::from_secs(300); // 5 minutes
        let mut current_backoff = initial_backoff;

        loop {
            // --- Phase 1: Registration ---
            let register_url = format!("{}/apps/{}", eureka_url, app_name);
            let payload = create_payload(&instance_id, &hostname, &ip, &app_name, port);

            tracing::debug!("🔌 Attempting to register with Eureka at {} (Backoff: {:?})", register_url, current_backoff);

            match client.post(&register_url).json(&payload).send().await {
                Ok(resp) => {
                    if resp.status().is_success() {
                        tracing::info!("Eureka Registration Successful!");
                        // Reset backoff on success
                        current_backoff = initial_backoff;
                    } else {
                        tracing::warn!("Registration failed with status: {}. Retrying in {:?}...", resp.status(), current_backoff);
                        sleep(current_backoff).await;
                        current_backoff = min(current_backoff * 2, max_backoff);
                        continue; // Retry registration
                    }
                }
                Err(e) => {
                    tracing::debug!("Eureka Connection Failed: {}. Retrying in {:?}...", e, current_backoff);
                    sleep(current_backoff).await;
                    current_backoff = min(current_backoff * 2, max_backoff);
                    continue; // Retry registration
                }
            }

            // --- Phase 2: Heartbeat Loop ---
            // If we are here, we are registered. Now we send heartbeats.
            let heartbeat_url = format!("{}/apps/{}/{}", eureka_url, app_name, instance_id);
            let mut interval = tokio::time::interval(Duration::from_secs(30));
            
            // Skip the first tick because we just registered
            interval.tick().await;

            loop {
                interval.tick().await; // Wait 30s

                match client.put(&heartbeat_url).send().await {
                    Ok(resp) => {
                        if resp.status() == reqwest::StatusCode::NOT_FOUND {
                            tracing::debug!("Eureka instance not found (404). It may have restarted. Re-registering...");
                            break; // Break inner loop -> Back to Registration Phase
                        } else if !resp.status().is_success() {
                            tracing::debug!("Heartbeat warning. Status: {}", resp.status());
                            // We stay in the loop for minor errors, but if it persists, Eureka might eventually evict us (404).
                        } else {
                            tracing::debug!("Heartbeat sent successfully.");
                        }
                    }
                    Err(e) => {
                        tracing::debug!("Heartbeat connection error: {}. Attempting to reconnect...", e);
                        break; // Break inner loop -> Back to Registration Phase
                    }
                }
            }
        }
    });
}

fn create_payload(instance_id: &str, hostname: &str, ip: &str, app_name: &str, port: u16) -> EurekaInstanceWrapper {
    let instance = InstanceInfo {
        instance_id: instance_id.to_string(),
        host_name: hostname.to_string(),
        app: app_name.to_string(),
        ip_addr: ip.to_string(),
        status: "UP".to_string(),
        port: PortInfo { port, enabled: "true".to_string() },
        secure_port: PortInfo { port: 443, enabled: "false".to_string() },
        data_center_info: DataCenterInfo {
            class: "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo".to_string(),
            name: "MyOwn".to_string(),
        },
        lease_info: LeaseInfo { duration_in_secs: 90 },
        metadata: MetaData { instance_id: instance_id.to_string() },
        home_page_url: format!("http://{}:{}/", ip, port),
        status_page_url: format!("http://{}:{}/info", ip, port),
        health_check_url: format!("http://{}:{}/health", ip, port),
        vip_address: app_name.to_string(),
        secure_vip_address: app_name.to_string(),
    };

    EurekaInstanceWrapper { instance }
}