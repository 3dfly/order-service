#!/bin/bash

echo "🔄 Monitoring for GitHub repository creation..."
echo "Please create the repository in your browser (it should be open now)"
echo ""
echo "Checking every 5 seconds..."

# Check if repository exists every 5 seconds
for i in {1..24}; do  # Check for 2 minutes maximum
    echo "Check $i/24: Checking if repository exists..."
    
    REPO_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://github.com/3dfly/order-service)
    
    if [ "$REPO_STATUS" = "200" ]; then
        echo "✅ Repository found! Pushing code..."
        sleep 2  # Wait a moment for repository to be fully ready
        
        if git push -u origin main; then
            echo ""
            echo "🎉 SUCCESS! Your code is now on GitHub!"
            echo "🌐 Repository URL: https://github.com/3dfly/order-service"
            echo ""
            echo "📋 What's next:"
            echo "1. ✅ Code is successfully on GitHub"
            echo "2. 🔧 Set up AWS deployment with: ./setup-aws.sh"
            echo "3. 🔐 Configure GitHub Actions secrets for CI/CD"
            echo ""
            break
        else
            echo "❌ Push failed - might be an authentication issue"
            echo "Please run: git push -u origin main"
            break
        fi
    fi
    
    echo "Repository not ready yet, waiting 5 seconds..."
    sleep 5
done

if [ "$REPO_STATUS" != "200" ]; then
    echo ""
    echo "⏰ Timeout waiting for repository creation."
    echo "Please create the repository manually at:"
    echo "https://github.com/new?name=order-service&owner=3dfly"
    echo ""
    echo "Then run: git push -u origin main"
fi 