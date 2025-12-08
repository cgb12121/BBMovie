#!/bin/bash
# BBMovie Master Launcher V5 (Delegated Mode)
# Equivalent to launcher.bat for Unix/Linux/macOS systems

# ==========================================
# 1. CONFIGURATION
# ==========================================
# Define services (Format: Folder_Name|Tech_Type)
services=(
    "eureka-sever|SPRING"
    "gateway|SPRING"
    "auth-service|SPRING"
    "file-service|SPRING"
    "ai-service|SPRING"
    "rust-ai-context-refinery|RUST"
    "watchlist-quarkus|QUARKUS"
    "payment-service|SPRING"
    "search-service|SPRING"
    "email-service|SPRING"
)

# ANSI Color codes
GREEN=$'\033[92m'
RED=$'\033[91m'
YELLOW=$'\033[93m'
CYAN=$'\033[96m'
WHITE=$'\033[97m'
RESET=$'\033[0m'

# ==========================================
# 2. LOAD GLOBAL ENV (Optional)
# ==========================================
echo -e "${YELLOW}[SYSTEM] Loading global .env variables...${RESET}"
if [ -f ".env" ]; then
    while IFS= read -r line; do
        # Skip comments and empty lines
        if [[ ! "$line" =~ ^#.*$ ]] && [[ -n "$line" ]]; then
            export "$line"
        fi
    done < .env
    echo -e "${GREEN}OK: Global .env loaded!${RESET}"
fi

# Function to display main menu
show_menu() {
    clear
    echo
    echo -e "${CYAN}=============================================================${RESET}"
    echo -e "${CYAN}   BBMovie Master Launcher V5 (Delegated Mode)              ${RESET}"
    echo -e "${CYAN}=============================================================${RESET}"
    echo
    echo -e "   [0]  ${RED}KILL ALL (Clean up)${RESET}"
    echo -e "   [A]  ${GREEN}Start ALL Services${RESET}"
    echo
    echo -e "   ${YELLOW}--- Select Specific Services ---${RESET}"
    echo

    # Table header
    printf "   +----+--------------------------------+-----------+\n"
    printf "   | ID | Service Name                   | Stack     |\n"
    printf "   +----+--------------------------------+-----------+\n"

    count=0
    for i in "${!services[@]}"; do
        ((i++))  # Array is 1-indexed like in the batch file
        service="${services[$((i-1))]}"
        if [ -n "$service" ]; then
            IFS='|' read -r svc_name svc_type <<< "$service"
            idx=$(printf "%2s" "$i")
            padded_name=$(printf "%-30s" "$svc_name")
            padded_type=$(printf "%-9s" "$svc_type")

            case "$svc_type" in
                "SPRING")
                    color="$GREEN"
                    ;;
                "RUST")
                    color="$YELLOW"
                    ;;
                "QUARKUS")
                    color="$CYAN"
                    ;;
                *)
                    color="$WHITE"
                    ;;
            esac

            printf "   | %2s | %s | %s%s${RESET} |\n" "$idx" "$padded_name" "$color" "$padded_type"
            count=$i
        fi
    done

    printf "   +----+--------------------------------+-----------+\n"
    echo
    echo -e "${CYAN}Example inputs:${RESET} \"1 2 5\" or \"A\""
    echo
}

# ==========================================
# 3. LOGIC HANDLERS
# ==========================================

process_selection() {
    local id="$1"
    if [ -z "$id" ]; then
        return
    fi
    
    # Check if id is valid
    if [[ $id -ge 1 && $id -le ${#services[@]} ]]; then
        service="${services[$((id-1))]}"
        IFS='|' read -r folder svc_type <<< "$service"
        launcher "$folder"
    else
        echo -e "${RED}Skipping invalid ID: [$id]${RESET}"
    fi
}

start_all() {
    # Run Eureka first
    launcher "eureka-sever"
    sleep 10
    for i in $(seq 2 $count); do
        service="${services[$((i-1))]}"
        if [ -n "$service" ]; then
            IFS='|' read -r folder svc_type <<< "$service"
            launcher "$folder"
        fi
    done
    echo
    echo -e "${GREEN}All services started.${RESET}"
    sleep 3
}

launcher() {
    local folder="$1"
    
    # Check if run.sh or run.bat exists
    if [ -f "$folder/run.sh" ]; then
        echo -e "${YELLOW}Delegating to $folder/run.sh...${RESET}"
        # Start service in background
        (
            cd "$folder" || exit
            ./run.sh &
        ) &
    elif [ -f "$folder/run.bat" ]; then
        echo -e "${YELLOW}Delegating to $folder/run.bat...${RESET}"
        # On Unix systems, we might need wine to run .bat files, but for now we'll just note this
        echo -e "${RED}Warning: $folder/run.bat exists but .bat files can't run directly on Unix systems${RESET}"
        echo -e "${YELLOW}Install wine if you need to run Windows batch files on Unix: sudo apt install wine${RESET}"
    else
        echo -e "${RED}Error: $folder/run.sh or $folder/run.bat not found! Skipping...${RESET}"
    fi
}

kill_all() {
    echo -e "${RED}Killing all java, cargo, rust processes...${RESET}"
    pkill -f java 2>/dev/null
    pkill -f cargo 2>/dev/null
    pkill -f ai-refinery 2>/dev/null
    pkill -f createdump 2>/dev/null
    echo -e "${GREEN}Cleaned up.${RESET}"
    sleep 2
}

# Main loop
while true; do
    show_menu
    read -p "Your choice: " choices
    
    if [ -z "$choices" ]; then
        continue
    fi
    
    if [ "$choices" = "0" ]; then
        kill_all
    elif [ "$choices" = "A" ] || [ "$choices" = "a" ]; then
        start_all
    else
        for id in $choices; do
            process_selection "$id"
        done
        
        echo
        echo -e "${GREEN}Services launched via delegated scripts.${RESET}"
        echo "Returning to Main Menu in 3 seconds..."
        sleep 3
    fi
done