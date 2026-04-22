# Azure Student VM — Ubuntu + Docker Setup Guide

A beginner-friendly guide to spinning up an Ubuntu VM on Azure Student subscription, connecting via SSH, and deploying Docker services (such as this bookstore application).

> **Note:** The instructions below use macOS examples for the host (your developer machine), but they can be easily adapted to Windows or Linux. The only thing required on your host machine is an SSH client that can authenticate with the Azure key file. On Windows, the built-in OpenSSH client (available in PowerShell) or tools like PuTTY work the same way. All commands executed on the VM itself (sections 4 onwards) are standard Linux commands — they will work identically regardless of your host OS, as long as you launched a Linux (Ubuntu) VM on Azure.

---

## Table of Contents

1. [Create an Ubuntu VM on Azure](#1-create-an-ubuntu-vm-on-azure)
2. [Connect via SSH from macOS](#2-connect-via-ssh-from-macos)
3. [Configure SSH for Easy Access](#3-configure-ssh-for-easy-access)
4. [Install Git](#4-install-git)
5. [Install Docker & Docker Compose](#5-install-docker--docker-compose)
6. [Network Security & Opening Ports](#6-network-security--opening-ports)
7. [Deploy the Bookstore Application](#7-deploy-the-bookstore-application)
8. [Cost Management — Stopping Your VM](#8-cost-management--stopping-your-vm)
9. [Quick Reference Cheatsheet](#quick-reference-cheatsheet)
10. [Terms and Abbreviations](#terms-and-abbreviations)

---

## 1. Create an Ubuntu VM on Azure

1. Go to [portal.azure.com](https://portal.azure.com) and sign in with your student account
2. Click **Create a resource** > **Virtual Machine**
3. Fill in the basics:
   - **Region**: Pick one close to you
   - **Image**: Ubuntu Server 22.04 LTS
   - **Size**: Something small works for most student projects (e.g. 2 vCPUs / 4 GB RAM)
   - **Authentication**: Choose **SSH public key**
   - **Username**: Azure will suggest `azureuser` — this is the standard default
4. Download the `.pem` key file when prompted — **keep it safe, you cannot get it again**
5. Click **Review + Create** > **Create**

> **Tip:** Your student subscription gives you free credits — check [Azure for Students](https://azure.microsoft.com/en-us/free/students/) for what is included.

---

## 2. Connect via SSH from macOS

### Step 1 — Secure your key file

```bash
# Move the key to your SSH directory
mv ~/Downloads/isp2026-01_key.pem ~/.ssh/

# Set correct permissions (SSH will refuse to use the key otherwise)
chmod 600 ~/.ssh/isp2026-01_key.pem
```

### Step 2 — Connect

```bash
ssh -i ~/.ssh/isp2026-01_key.pem azureuser@<YOUR_VM_IP>
```

Replace `<YOUR_VM_IP>` with your VM's public IP address (visible in the Azure Portal under your VM's **Overview** tab).

> **Tip:** The default username for Azure Ubuntu VMs is `azureuser`. If that does not work, check the Azure Portal > VM > Overview.

### Common SSH Errors

| Error | Fix |
|-------|-----|
| `WARNING: UNPROTECTED PRIVATE KEY FILE` | Run `chmod 600 ~/.ssh/your-key.pem` |
| `Permission denied (publickey)` | Wrong username — double-check in Azure Portal |
| `Connection timed out` | Port 22 is blocked — see [Network Security](#6-network-security--opening-ports) |

---

## 3. Configure SSH for Easy Access

Instead of typing the full command every time, save a shortcut in your SSH config.

```bash
nano ~/.ssh/config
```

Add this block (replace values with yours):

```
Host my-azure-vm
    HostName      <YOUR_VM_IP>
    User          azureuser
    IdentityFile  ~/.ssh/isp2026-01_key.pem
    IdentitiesOnly yes
```

Save with `Ctrl+O` > `Enter` > `Ctrl+X`, then secure the config file:

```bash
chmod 600 ~/.ssh/config
```

Now connect with just:

```bash
ssh my-azure-vm
```

---

## 4. Install Git

> **Run on: Azure VM** — connect via SSH first (`ssh my-azure-vm`).

```bash
sudo apt-get update
sudo apt-get install -y git

# Verify
git --version
```

---

## 5. Install Docker & Docker Compose

> **Run on: Azure VM** — all commands in this section are executed on the remote VM via SSH.

### Step 1 — Update and install dependencies

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
```

### Step 2 — Add Docker's official GPG key and repository

```bash
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

### Step 3 — Install Docker Engine

```bash
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### Step 4 — Run Docker without `sudo`

```bash
sudo usermod -aG docker $USER
newgrp docker
```

### Verify the installation

```bash
docker --version        # Should print Docker version
docker compose version  # Should print Docker Compose version
docker run hello-world  # Should run a test container
```

> **Note:** Docker Compose is now built into Docker as a plugin. Use `docker compose` (space) instead of the old `docker-compose` (hyphen).

---

## 6. Network Security & Opening Ports

> **Run on: Azure Portal** (web browser) — these are Azure networking settings, not VM shell commands.

When you run Docker services, they listen on ports (e.g. port 80 for a web app, 8080 for this bookstore API). Azure blocks all inbound ports by default — you need to open them manually.

### Check Current Rules

1. Go to **Azure Portal** > Your VM > **Networking** > **Network security group**
2. You will see **Inbound port rules** — by default only port **22 (SSH)** is open

### Open a Port (Azure Portal)

For this bookstore application, you only need to open port **8080** — that is the only port required to access the application remotely from your browser. PostgreSQL (port 5432) runs inside Docker's internal network and does not need to be exposed.

1. Click **Add inbound port rule**
2. Fill in:
   - **Source**: Any (or your IP for extra security)
   - **Destination port ranges**: `8080`
   - **Protocol**: TCP
   - **Action**: Allow
   - **Priority**: any number (lower = higher priority)
   - **Name**: `allow-app-port`
3. Click **Add**

### Common Ports to Open

| Service | Port |
|---------|------|
| HTTP (web) | 80 |
| HTTPS (secure web) | 443 |
| **Alternative HTTP / Spring Boot** | **8080** |
| PostgreSQL | 5432 |
| MySQL | 3306 |
| MongoDB | 27017 |

> **Security tip:** Only open ports you actually need. Avoid opening port ranges or using `*` (any port) for inbound rules in production environments.

### Verify a Port is Reachable (from your local machine)

```bash
nc -zv <YOUR_VM_IP> 8080
```

---

## 7. Deploy the Bookstore Application

> **Run on: Azure VM** — all commands in this section are executed on the remote VM via SSH.

Once Docker is installed and port 8080 is open, deploy the bookstore on your VM.

### Step 1 — Install Git (if not already installed)

```bash
sudo apt-get update
sudo apt-get install -y git
git --version   # Verify installation
```

### Step 2 — Clone and start the application

```bash
# Clone the repository
git clone <YOUR_REPO_URL>
cd 06-bookstore-api-collections

# Build and start (PostgreSQL + app)
docker compose up -d

# Verify
docker ps                    # Both containers should be running
curl http://localhost:8080    # Should return the index page
```

The application is now accessible at `http://<YOUR_VM_IP>:8080` from any browser.

To stop:

```bash
docker compose down       # Stop containers
docker compose down -v    # Stop and remove database volume
```

For details on the application itself, see:
- **[README.md](README.md)** — Quick start and build commands
- **[DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md)** — Architecture, API reference, and glossary
- **[COLLECTIONS-GUIDE.md](COLLECTIONS-GUIDE.md)** — Java Collections patterns walkthrough

---

## 8. Cost Management — Stopping Your VM

### Always stop from the Azure Portal

Click **Stop** in the Azure Portal. This **deallocates** the VM — you stop paying for compute.

> **Warning:** If you run `sudo shutdown now` from inside the VM, it stops the OS but **the VM stays allocated** and **you keep getting charged**.

### What you pay while stopped (deallocated)

| Resource | Cost |
|----------|------|
| Compute (CPU/RAM) | Free — no charge |
| OS Disk (storage) | ~$2-3/month |
| Public IP (static) | ~$3-4/month |
| **Total while stopped** | **~$5-7/month** |

### Check the state in the Portal

Your VM should show **"Stopped (deallocated)"** — not just "Stopped".

### Save even more

If you do not need to keep the same IP address, **release the public IP** when stopped to save an extra ~$3-4/month. When you restart, Azure assigns a new IP — just update your SSH config.

---

## Quick Reference Cheatsheet

```bash
# SSH connect (after config setup)
ssh my-azure-vm

# Update system
sudo apt-get update && sudo apt-get upgrade -y

# Docker basics
docker ps                        # List running containers
docker ps -a                     # List all containers
docker images                    # List images
docker compose up -d             # Start services in background
docker compose down              # Stop services
docker compose logs -f           # Follow logs

# Check open ports on the VM
sudo ss -tlnp                    # Show listening ports
```

---

## Terms and Abbreviations

| Term / Abbreviation | Definition |
|---------------------|-----------|
| Azure | Microsoft's cloud computing platform providing VMs, networking, storage, and other infrastructure services |
| CLI | Command-Line Interface — a text-based tool for interacting with software (e.g. Azure CLI, Docker CLI) |
| containerd | An industry-standard container runtime that Docker Engine uses under the hood |
| Deallocate | Release all compute resources (CPU, RAM) for a VM so you are no longer billed for them |
| Docker | A platform for building, shipping, and running applications in isolated containers |
| Docker Compose | A tool for defining and running multi-container Docker applications via a `compose.yml` file |
| GPG | GNU Privacy Guard — a tool for encrypting data and verifying software package authenticity |
| LTS | Long-Term Support — an Ubuntu release that receives security updates for 5 years |
| NSG | Network Security Group — Azure's virtual firewall that controls inbound/outbound traffic to VMs |
| PEM | Privacy Enhanced Mail — a file format for cryptographic keys and certificates (used here for SSH keys) |
| SSH | Secure Shell — an encrypted protocol for remote command-line access to servers |
| TCP | Transmission Control Protocol — a reliable, connection-oriented network protocol used by HTTP, SSH, and database connections |
| VM | Virtual Machine — a software-emulated computer running on shared physical hardware in the cloud |
| vCPU | Virtual CPU — a share of a physical processor core allocated to a VM |

