#!/bin/bash
echo "=== STEP 2: REMOTE FULL CLEAN + RESYNC ==="
echo "Cleaning all submodule src directories on remote..."

for mod in bx-ai bx-billing bx-common bx-content bx-data bx-gateway bx-lead bx-message bx-monitor bx-risk bx-schedule bx-social bx-storage bx-system bx-task bx-tenant bx-user; do
    echo "Deleting /opt/beijixing-ai/backend/$mod/src ..."
    rm -rf /opt/beijixing-ai/backend/$mod/src
done

echo "Also cleaning beijixing-app/src ..."
rm -rf /opt/beijixing-ai/backend/beijixing-app/src

echo "Cleaning stale BOOT-INF directory..."
rm -rf /opt/beijixing-ai/backend/beijixing-app/BOOT-INF

echo "All submodule src directories deleted."
echo "Ready for resync from local."
