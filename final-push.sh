#!/bin/bash

echo "🚀 Final GitHub Push - Complete Solution"
echo "======================================="

# Check repository exists
REPO_CHECK=$(curl -s -o /dev/null -w "%{http_code}" https://github.com/3dfly/order-service)
if [ "$REPO_CHECK" != "200" ]; then
    echo "❌ Repository not found on GitHub. Please create it first."
    exit 1
fi

echo "✅ Repository exists on GitHub: https://github.com/3dfly/order-service"

# Set up authentication using GitHub CLI if available
if command -v gh > /dev/null 2>&1; then
    echo "🔐 GitHub CLI detected, using for authentication..."
    if gh auth status > /dev/null 2>&1; then
        echo "✅ Already authenticated with GitHub CLI"
        git remote set-url origin https://github.com/3dfly/order-service.git
        git push -u origin main
        if [ $? -eq 0 ]; then
            echo ""
            echo "🎉 SUCCESS! Code pushed to GitHub!"
            echo "🌐 Repository: https://github.com/3dfly/order-service"
            exit 0
        fi
    else
        echo "🔐 Authenticating with GitHub CLI..."
        gh auth login --web
        git remote set-url origin https://github.com/3dfly/order-service.git
        git push -u origin main
        if [ $? -eq 0 ]; then
            echo ""
            echo "🎉 SUCCESS! Code pushed to GitHub!"
            echo "🌐 Repository: https://github.com/3dfly/order-service"
            exit 0
        fi
    fi
fi

# Alternative: Use personal access token
echo ""
echo "🔑 AUTHENTICATION REQUIRED"
echo "=========================="
echo ""
echo "GitHub requires authentication to push code. Here are your options:"
echo ""
echo "OPTION 1 (Recommended): Personal Access Token"
echo "1. Go to: https://github.com/settings/tokens"
echo "2. Click 'Generate new token (classic)'"
echo "3. Give it a name like 'order-service'"
echo "4. Check 'repo' scope"
echo "5. Copy the token"
echo "6. When Git asks for password, paste the token"
echo ""
echo "OPTION 2: GitHub CLI"
echo "1. Install: brew install gh"
echo "2. Run: gh auth login"
echo "3. Follow the prompts"
echo ""

# Try push with token prompt
echo "Attempting push (you'll need to enter your GitHub username and token)..."
git remote set-url origin https://github.com/3dfly/order-service.git

# Create a wrapper that handles the push
cat > push_with_auth.sh << 'EOF'
#!/bin/bash
echo "Enter your GitHub username (3dfly):"
read -r USERNAME
echo "Enter your Personal Access Token (from https://github.com/settings/tokens):"
read -s TOKEN
echo ""

git remote set-url origin https://$USERNAME:$TOKEN@github.com/3dfly/order-service.git
git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 SUCCESS! Code pushed to GitHub!"
    echo "🌐 Repository: https://github.com/3dfly/order-service"
    # Clean up the token from git config for security
    git remote set-url origin https://github.com/3dfly/order-service.git
else
    echo "❌ Push failed. Please check your token and try again."
fi
EOF

chmod +x push_with_auth.sh
echo ""
echo "🔐 Running secure authentication script..."
./push_with_auth.sh

# Clean up
rm -f push_with_auth.sh 