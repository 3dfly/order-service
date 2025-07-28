#!/bin/bash

echo "🚀 GitHub Repository Setup Script"
echo "================================="

# Check if we're in the right directory
if [ ! -f "README.md" ] || [ ! -f "build.gradle" ]; then
    echo "❌ Error: Not in the order-service directory!"
    echo "Please run this script from /Users/sefica/Downloads/order-service"
    exit 1
fi

# Check Git status
if ! git status > /dev/null 2>&1; then
    echo "❌ Error: Not a Git repository!"
    exit 1
fi

echo "✅ In correct directory and Git repository detected"

# Check current remotes
echo "🔍 Checking current Git remotes..."
CURRENT_REMOTE=$(git remote -v)
echo "$CURRENT_REMOTE"

# Check if repository exists on GitHub
echo "🌐 Checking if GitHub repository exists..."
REPO_CHECK=$(curl -s -o /dev/null -w "%{http_code}" https://github.com/3dfly/order-service)

if [ "$REPO_CHECK" = "404" ]; then
    echo "❌ Repository doesn't exist on GitHub yet!"
    echo ""
    echo "🛠️  SOLUTION: I'll open the repository creation page for you..."
    echo ""
    echo "📋 Follow these exact steps:"
    echo "1. I'll open GitHub in your browser"
    echo "2. Make sure you're signed in to your '3dfly' account"
    echo "3. The form will be pre-filled - just click 'Create repository'"
    echo "4. Come back here and press Enter to continue"
    echo ""
    
    # Create the GitHub repository URL with pre-filled form
    GITHUB_URL="https://github.com/new?name=order-service&description=Spring+Boot+microservice+for+order+management+with+AWS+deployment&owner=3dfly"
    
    echo "🌐 Opening GitHub repository creation page..."
    open "$GITHUB_URL" 2>/dev/null || echo "Please manually go to: $GITHUB_URL"
    
    echo ""
    echo "⏳ After creating the repository on GitHub, press Enter to continue..."
    read -r
    
    # Check again if repository was created
    echo "🔄 Checking if repository was created..."
    REPO_CHECK_2=$(curl -s -o /dev/null -w "%{http_code}" https://github.com/3dfly/order-service)
    
    if [ "$REPO_CHECK_2" = "404" ]; then
        echo "❌ Repository still not found. Please create it manually:"
        echo "   1. Go to https://github.com/new"
        echo "   2. Repository name: order-service"
        echo "   3. Owner: 3dfly"
        echo "   4. Description: Spring Boot microservice for order management with AWS deployment"
        echo "   5. DON'T check any boxes (no README, .gitignore, or license)"
        echo "   6. Click 'Create repository'"
        echo ""
        echo "Then run this script again."
        exit 1
    fi
fi

echo "✅ Repository exists on GitHub!"

# Set up the remote if not already set
if ! git remote get-url origin > /dev/null 2>&1; then
    echo "🔗 Adding GitHub remote..."
    git remote add origin https://github.com/3dfly/order-service.git
else
    echo "🔗 Updating GitHub remote..."
    git remote set-url origin https://github.com/3dfly/order-service.git
fi

# Verify remote is correct
echo "🔍 Verifying remote configuration..."
git remote -v

# Check if we have commits to push
COMMITS=$(git log --oneline 2>/dev/null | wc -l | tr -d ' ')
if [ "$COMMITS" -eq 0 ]; then
    echo "❌ No commits found! Creating initial commit..."
    git add .
    git commit -m "feat: initial Spring Boot order service with AWS deployment"
fi

echo "📤 Pushing code to GitHub..."
if git push -u origin main; then
    echo ""
    echo "🎉 SUCCESS! Your code is now on GitHub!"
    echo "🌐 Repository URL: https://github.com/3dfly/order-service"
    echo ""
    echo "📋 Next steps:"
    echo "1. ✅ Code is on GitHub"
    echo "2. 🔧 Set up AWS deployment (run ./setup-aws.sh)"
    echo "3. 🔐 Configure GitHub Actions secrets for CI/CD"
    echo ""
else
    echo "❌ Push failed! This might be an authentication issue."
    echo ""
    echo "🔧 Troubleshooting:"
    echo "1. Make sure you're logged into GitHub in your browser"
    echo "2. You might need to set up Git authentication:"
    echo "   - Personal Access Token: https://github.com/settings/tokens"
    echo "   - Or SSH keys: https://docs.github.com/en/authentication/connecting-to-github-with-ssh"
    echo ""
    echo "After setting up authentication, run:"
    echo "   git push -u origin main"
fi 