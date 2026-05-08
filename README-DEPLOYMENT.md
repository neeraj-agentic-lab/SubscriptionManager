# Multi-Cloud Deployment Guide

This guide explains how to deploy the Subscription Manager application to Google Cloud Platform (GCP), Amazon Web Services (AWS), or Microsoft Azure.

## Quick Start

### Option 1: Automatic Deployment (Config File)

1. **Edit deployment configuration:**
   ```bash
   # Edit deployment-config.yaml
   target_cloud: gcp  # Change to: gcp, aws, or azure
   ```

2. **Commit and push to GitHub:**
   ```bash
   git add deployment-config.yaml
   git commit -m "Deploy to GCP"
   git push origin main
   ```

3. **GitHub Actions automatically deploys** to your chosen cloud!

### Option 2: Manual Deployment (GitHub UI)

1. Go to your GitHub repository
2. Click **Actions** tab
3. Select **"Manual Deploy (UI)"** workflow
4. Click **"Run workflow"**
5. Select options from dropdowns:
   - Target Cloud: GCP / AWS / Azure
   - Environment: production / staging / development
   - Components: API, Worker, or both
6. Click **"Run workflow"**

### Option 3: Local Deployment

```bash
# Run deployment script locally
./infrastructure/deploy.sh
```

---

## Prerequisites

### 1. Cloud Account Setup

You need an account on your target cloud provider:
- **GCP:** Google Cloud Platform account with billing enabled
- **AWS:** Amazon Web Services account with IAM access
- **Azure:** Microsoft Azure subscription

### 2. GitHub Secrets Configuration

Add the following secrets to your GitHub repository (Settings → Secrets and variables → Actions):

#### For GCP Deployment:
```
GCP_SA_KEY          = <service-account-json-key>
GCP_PROJECT_ID      = <your-gcp-project-id>
```

#### For AWS Deployment:
```
AWS_ACCESS_KEY_ID       = <your-aws-access-key>
AWS_SECRET_ACCESS_KEY   = <your-aws-secret-key>
AWS_REGION              = us-east-1
```

#### For Azure Deployment:
```
AZURE_CREDENTIALS = {
  "clientId": "...",
  "clientSecret": "...",
  "subscriptionId": "...",
  "tenantId": "..."
}
```

### 3. Required Tools (for local deployment)

- Docker
- Cloud CLI tools:
  - GCP: `gcloud` CLI
  - AWS: `aws` CLI
  - Azure: `az` CLI
- `yq` (YAML processor)

---

## Cloud-Specific Setup

### GCP Setup

1. **Create service account:**
   ```bash
   gcloud iam service-accounts create github-deployer \
     --display-name="GitHub Actions Deployer"
   ```

2. **Grant permissions:**
   ```bash
   gcloud projects add-iam-policy-binding PROJECT_ID \
     --member="serviceAccount:github-deployer@PROJECT_ID.iam.gserviceaccount.com" \
     --role="roles/run.admin"
   
   gcloud projects add-iam-policy-binding PROJECT_ID \
     --member="serviceAccount:github-deployer@PROJECT_ID.iam.gserviceaccount.com" \
     --role="roles/cloudsql.admin"
   
   gcloud projects add-iam-policy-binding PROJECT_ID \
     --member="serviceAccount:github-deployer@PROJECT_ID.iam.gserviceaccount.com" \
     --role="roles/secretmanager.admin"
   ```

3. **Create key:**
   ```bash
   gcloud iam service-accounts keys create key.json \
     --iam-account=github-deployer@PROJECT_ID.iam.gserviceaccount.com
   ```

4. **Add to GitHub Secrets:**
   - Copy content of `key.json` to `GCP_SA_KEY` secret
   - Add your project ID to `GCP_PROJECT_ID` secret

### AWS Setup

1. **Create IAM user:**
   ```bash
   aws iam create-user --user-name github-deployer
   ```

2. **Attach policies:**
   ```bash
   aws iam attach-user-policy \
     --user-name github-deployer \
     --policy-arn arn:aws:iam::aws:policy/AmazonECS_FullAccess
   
   aws iam attach-user-policy \
     --user-name github-deployer \
     --policy-arn arn:aws:iam::aws:policy/AmazonRDSFullAccess
   ```

3. **Create access key:**
   ```bash
   aws iam create-access-key --user-name github-deployer
   ```

4. **Add to GitHub Secrets:**
   - Add Access Key ID to `AWS_ACCESS_KEY_ID`
   - Add Secret Access Key to `AWS_SECRET_ACCESS_KEY`

### Azure Setup

1. **Create service principal:**
   ```bash
   az ad sp create-for-rbac \
     --name "github-deployer" \
     --role contributor \
     --scopes /subscriptions/SUBSCRIPTION_ID \
     --sdk-auth
   ```

2. **Add to GitHub Secrets:**
   - Copy entire JSON output to `AZURE_CREDENTIALS` secret

---

## Configuration

### deployment-config.yaml

Edit this file to configure your deployment:

```yaml
# Choose your cloud
target_cloud: gcp  # gcp | aws | azure

# Database settings
database:
  tier: small      # small | medium | large
  storage_gb: 10

# API settings
api:
  compute:
    size: small
    min_instances: 1
    max_instances: 10

# Worker settings
worker:
  compute:
    size: small
  schedule: "0 */1 * * *"  # Cron expression
```

### Compute Size Mapping

| Size | GCP | AWS | Azure |
|------|-----|-----|-------|
| **small** | 1 vCPU, 2GB | t3.small | B1ms |
| **medium** | 2 vCPU, 4GB | t3.medium | B2s |
| **large** | 4 vCPU, 8GB | t3.large | B4ms |

### Database Tier Mapping

| Tier | GCP | AWS | Azure |
|------|-----|-----|-------|
| **small** | db-f1-micro | db.t3.micro | B_Gen5_1 |
| **medium** | db-g1-small | db.t3.small | GP_Gen5_2 |
| **large** | db-n1-standard-1 | db.t3.medium | GP_Gen5_4 |

---

## Deployment Workflows

### Full Deployment

Deploys database, API, and Worker:

```bash
./infrastructure/deploy.sh
```

### API Only

```bash
./infrastructure/deploy-api.sh
```

### Worker Only

```bash
./infrastructure/deploy-worker.sh
```

---

## Monitoring & Logs

### GCP
- **API Logs:** Cloud Run → subscription-api → Logs
- **Worker Logs:** Cloud Run Jobs → subscription-worker → Logs
- **Database:** Cloud SQL → subscription-db → Monitoring

### AWS
- **API Logs:** ECS → Clusters → subscription-cluster → subscription-api → Logs
- **Worker Logs:** ECS → Scheduled Tasks → subscription-worker → Logs
- **Database:** RDS → Databases → subscription-db → Monitoring

### Azure
- **API Logs:** Container Apps → subscription-api → Log stream
- **Worker Logs:** Container Apps Jobs → subscription-worker → Logs
- **Database:** Azure Database for PostgreSQL → subscription-db → Metrics

---

## Troubleshooting

### Deployment Fails

1. **Check GitHub Actions logs:**
   - Go to Actions tab
   - Click on failed workflow
   - Review error messages

2. **Verify secrets:**
   - Ensure all required secrets are configured
   - Check secret names match exactly

3. **Check cloud quotas:**
   - GCP: IAM & Admin → Quotas
   - AWS: Service Quotas
   - Azure: Subscriptions → Usage + quotas

### Database Connection Issues

1. **Verify database is running:**
   ```bash
   # GCP
   gcloud sql instances describe subscription-db
   
   # AWS
   aws rds describe-db-instances --db-instance-identifier subscription-db
   
   # Azure
   az postgres flexible-server show --name subscription-db
   ```

2. **Check secrets:**
   ```bash
   # GCP
   gcloud secrets versions access latest --secret=db-password
   
   # AWS
   aws secretsmanager get-secret-value --secret-id db-password
   
   # Azure
   az keyvault secret show --vault-name myvault --name db-password
   ```

### Migration Failures

Migrations run via Gradle Flyway. To run manually:

```bash
./gradlew flywayMigrate \
  -Dflyway.url="jdbc:postgresql://HOST:5432/subscription_db" \
  -Dflyway.user="subscription_app" \
  -Dflyway.password="PASSWORD"
```

---

## Cleanup

To delete all cloud resources:

```bash
# GCP
./infrastructure/gcp/cleanup.sh

# AWS
./infrastructure/aws/cleanup.sh

# Azure
./infrastructure/azure/cleanup.sh
```

**Warning:** This deletes everything including the database!

---

## Cost Estimation

### Development Environment (small tier)
- **GCP:** ~$15/month
- **AWS:** ~$20/month
- **Azure:** ~$18/month

### Production Environment (medium tier)
- **GCP:** ~$80/month
- **AWS:** ~$100/month
- **Azure:** ~$90/month

---

## Support

For issues or questions:
1. Check GitHub Actions logs
2. Review cloud provider console
3. Check application logs
4. Verify configuration in `deployment-config.yaml`

---

## Architecture

```
GitHub Repository
    ↓
GitHub Actions (CI/CD)
    ↓
    ├─→ Run Database Migrations (Gradle Flyway)
    ├─→ Build Docker Images (API + Worker)
    ├─→ Push to Cloud Registry
    └─→ Deploy to Cloud
        ├─→ API (Serverless Container)
        ├─→ Worker (Scheduled Jobs)
        └─→ Database (Managed PostgreSQL)
```

---

## Next Steps

1. ✅ Configure GitHub Secrets
2. ✅ Edit `deployment-config.yaml`
3. ✅ Push to GitHub
4. ✅ Watch automatic deployment
5. ✅ Verify application is running
6. ✅ Check logs and monitoring

Happy deploying! 🚀
