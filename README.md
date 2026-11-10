# Asklepios Gateway Service

This is the **gateway layer** for the Asklepios platform. It handles:
- **Authentication**
- **JWT generation**
- **Facility-based role/authority resolution**
- **Routing to downstream microservices**

# Architecture Overview

## 🛂 Authentication Model

The authentication model is **multi-facility and role-aware**.

### 🔗 Entity Relationships

- `facility` → has many → `roles`
- `role` → mapped to → multiple `authorities`
- `user` → assigned → roles per facility

Each role is **scoped to a specific facility**, so users can have different permissions in different facilities.

### 🧠 Business Logic

> A user must provide their `facilityId` at login.  
> Only the roles assigned to that user **within the selected facility** are used to resolve authorities.

---

### 📦 JWT Token Structure

On successful authentication, the API returns a **JWT token** with:

- `sub`: the username (login)
- `tenant`: the selected facility context
- `authorities`: resolved from roles assigned in the facility
- `iat`, `exp`: token issue and expiry

## ⚙️ Stateless Design

The gateway is fully stateless:
- No HTTP sessions or cookies are used.
- All authentication and authorization data is included in the JWT token.
- Clients are responsible for sending the token with every request.

---

## 🛡️ Security Summary

Key security design principles:
- Passwords are stored hashed
- JWTs are signed and include an **expiration time**
- **Facility ID** is embedded in the token to ensure tenant isolation
- All requests must include a valid JWT (`Authorization` header)
- Gateway and services are designed with **zero trust** between users and systems

dummy
