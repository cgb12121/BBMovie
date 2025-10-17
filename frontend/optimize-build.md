# Build Performance Optimization Guide

## Current Issues:
- **First load time:** 2-3 minutes
- **Cause:** Loading 3 UI frameworks + 24 Radix packages + TypeScript compilation

## Quick Wins (Do Now):

### Option 1: Remove Unused Radix UI Packages (Recommended - Safe & Fast)
```bash
npm uninstall @radix-ui/react-accordion @radix-ui/react-alert-dialog @radix-ui/react-aspect-ratio @radix-ui/react-collapsible @radix-ui/react-context-menu @radix-ui/react-dialog @radix-ui/react-dropdown-menu @radix-ui/react-hover-card @radix-ui/react-menubar @radix-ui/react-navigation-menu @radix-ui/react-popover @radix-ui/react-radio-group @radix-ui/react-toggle @radix-ui/react-toggle-group @radix-ui/react-tooltip
```

**Expected improvement:** 30-40 seconds faster (removes ~16 unused packages)

### Option 2: Optimize Tailwind Content Scanning
Already optimized, but you can add to tsconfig.json:
```json
"exclude": ["node_modules", "build", "dist"]
```

### Option 3: Enable webpack caching (CRA 5+)
Check if you can add to package.json:
```json
"scripts": {
  "start": "GENERATE_SOURCEMAP=false react-scripts start"
}
```

## Medium-Term Optimizations:

### Option 4: Migrate Remaining Pages
You still have unmigrated pages using old UI:
- FileUpload.tsx (Material-UI)
- FileManagement.tsx (Material-UI) 
- FileUploadManager.tsx (Material-UI)
- FilePreview.tsx (Material-UI)
- Navbar, SearchBar, UserMenu (using Ant Design styled-components)

Once migrated, you can remove:
```bash
npm uninstall antd @ant-design/icons @mui/material @mui/icons-material @emotion/react @emotion/styled styled-components
```

**Expected improvement:** 60-90 seconds faster

## Advanced Optimizations:

### Option 5: Use CRACO for Custom webpack Config
Install CRACO to customize Create React App:
```bash
npm install -D @craco/craco
```

Add persistent caching and optimizations in craco.config.js

### Option 6: Consider Vite Migration
Vite is 10-20x faster than webpack for dev startup:
- First load: ~5-10 seconds instead of 2-3 minutes
- Hot reload: instant instead of 2-3 seconds

## Immediate Action Plan:

**Step 1:** Remove unused Radix packages (saves ~40 seconds)
**Step 2:** Disable source maps in dev (saves ~20 seconds)  
**Step 3:** Migrate remaining 8 pages (saves ~90 seconds)
**Step 4:** Remove old UI frameworks (saves additional ~60 seconds)

**Total potential improvement:** 2-3 minutes → 30-45 seconds

## Temporary Workaround:

Keep the dev server running! Once started, hot reload is fast. Only the first cold start is slow.

