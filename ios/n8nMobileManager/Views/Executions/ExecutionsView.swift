//
//  ExecutionsView.swift
//  n8nMobileManager
//

import SwiftUI

struct ExecutionsView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = ExecutionsViewModel()
    @State private var selectedStatus: ExecutionStatus?
    @State private var selectedExecution: Execution?
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                filterBar
                executionsList
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle("Exécutions")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { Task { await viewModel.refresh() } }) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .refreshable { await viewModel.refresh() }
            .task { if let instance = appState.currentInstance { await viewModel.load(instance: instance) } }
            .sheet(item: $selectedExecution) { execution in
                ExecutionDetailView(executionId: execution.id, viewModel: viewModel)
            }
        }
    }
    
    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChip(title: "Tous", isSelected: selectedStatus == nil) { selectedStatus = nil }
                FilterChip(title: "Succès", isSelected: selectedStatus == .success, color: .n8nGreen) { selectedStatus = .success }
                FilterChip(title: "Erreurs", isSelected: selectedStatus == .error, color: .n8nRed) { selectedStatus = .error }
                FilterChip(title: "En cours", isSelected: selectedStatus == .running, color: .n8nBlue) { selectedStatus = .running }
            }.padding()
        }.background(Color(UIColor.secondarySystemBackground))
    }
    
    private var executionsList: some View {
        Group {
            if viewModel.isLoading && viewModel.executions.isEmpty {
                ProgressView("Chargement...").frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if filteredExecutions.isEmpty {
                EmptyStateView(icon: "clock.arrow.circlepath", message: "Aucune exécution")
            } else {
                List {
                    ForEach(filteredExecutions) { execution in
                        ExecutionListItem(execution: execution)
                            .contentShape(Rectangle())
                            .onTapGesture { selectedExecution = execution }
                    }
                }.listStyle(.insetGrouped)
            }
        }
    }
    
    private var filteredExecutions: [Execution] {
        guard let status = selectedStatus else { return viewModel.executions }
        return viewModel.executions.filter { $0.status == status }
    }
}

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    var color: Color = .n8nOrange
    let action: () -> Void
    
    var body: some View {
        Button(action: { HapticFeedback.selection.trigger(); action() }) {
            Text(title).font(.subheadline.weight(.medium))
                .foregroundColor(isSelected ? .white : .primary)
                .padding(.horizontal, 16).padding(.vertical, 8)
                .background(isSelected ? color : Color(UIColor.tertiarySystemBackground))
                .clipShape(Capsule())
        }
    }
}

struct ExecutionListItem: View {
    let execution: Execution
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(statusColor.opacity(0.15)).frame(width: 40, height: 40)
                Image(systemName: execution.status.icon).foregroundColor(statusColor)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(execution.workflowName ?? "Workflow \(execution.workflowId)").font(.subheadline.weight(.medium)).lineLimit(1)
                HStack(spacing: 8) {
                    Label(execution.mode.displayName, systemImage: execution.mode.icon).font(.caption2)
                    Text(execution.startedAt.timeAgo()).font(.caption2)
                }.foregroundColor(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(execution.status.displayName).font(.caption.weight(.medium)).foregroundColor(statusColor)
                Text(execution.formattedDuration).font(.caption2).foregroundColor(.secondary)
            }
        }.padding(.vertical, 4)
    }
    
    private var statusColor: Color {
        switch execution.status {
        case .success: return .n8nGreen
        case .error, .crashed: return .n8nRed
        case .running: return .n8nBlue
        case .waiting: return .n8nYellow
        default: return .secondary
        }
    }
}

struct ExecutionDetailView: View {
    let executionId: String
    @ObservedObject var viewModel: ExecutionsViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var detail: ExecutionDetail?
    @State private var isLoading = true
    
    var body: some View {
        NavigationStack {
            ScrollView {
                if isLoading {
                    ProgressView().padding(.top, 50)
                } else if let detail = detail {
                    detailContent(detail)
                } else {
                    Text("Impossible de charger les détails").foregroundColor(.secondary)
                }
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle("Exécution")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Fermer") { dismiss() } } }
            .task { await loadDetail() }
        }
    }
    
    private func detailContent(_ detail: ExecutionDetail) -> some View {
        VStack(spacing: 20) {
            // Status card
            VStack(spacing: 16) {
                HStack {
                    ZStack {
                        Circle().fill(statusColor(detail.status).opacity(0.15)).frame(width: 50, height: 50)
                        Image(systemName: detail.status.icon).font(.title2).foregroundColor(statusColor(detail.status))
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        Text(detail.status.displayName).font(.title3.bold())
                        Text(detail.workflowName ?? "Workflow").font(.subheadline).foregroundColor(.secondary)
                    }
                    Spacer()
                }
                Divider()
                HStack {
                    InfoItem(title: "Démarré", value: detail.startedAt.formattedDateTime())
                    Spacer()
                    InfoItem(title: "Mode", value: detail.mode.displayName)
                }
            }.padding().background(Color(UIColor.secondarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 16))
            
            // Actions
            if detail.status == .error {
                Button(action: { retryExecution() }) {
                    HStack { Image(systemName: "arrow.clockwise"); Text("Réessayer") }
                }.buttonStyle(PrimaryButtonStyle())
            }
        }.padding()
    }
    
    private func statusColor(_ status: ExecutionStatus) -> Color {
        switch status {
        case .success: return .n8nGreen
        case .error, .crashed: return .n8nRed
        case .running: return .n8nBlue
        default: return .secondary
        }
    }
    
    private func loadDetail() async {
        detail = await viewModel.getExecutionDetail(id: executionId)
        isLoading = false
    }
    
    private func retryExecution() {
        Task {
            await viewModel.retryExecution(id: executionId)
            dismiss()
        }
    }
}

struct InfoItem: View {
    let title: String; let value: String
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.caption).foregroundColor(.secondary)
            Text(value).font(.subheadline.weight(.medium))
        }
    }
}
