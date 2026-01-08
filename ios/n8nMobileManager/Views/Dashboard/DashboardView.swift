//
//  DashboardView.swift
//  n8nMobileManager
//

import SwiftUI
import Charts

struct DashboardView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = DashboardViewModel()
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    instanceStatusCard
                    statsGrid
                    recentExecutionsSection
                    topWorkflowsSection
                }
                .padding()
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle("Dashboard")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { Task { await viewModel.refresh() } }) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .refreshable { await viewModel.refresh() }
            .task { if let instance = appState.currentInstance { await viewModel.load(instance: instance) } }
        }
    }
    
    private var instanceStatusCard: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(appState.currentInstance?.name ?? "Instance").font(.title2.bold())
                    Text(appState.currentInstance?.baseURL ?? "").font(.caption).foregroundColor(.secondary)
                }
                Spacer()
                StatusBadge(status: viewModel.instanceStatus)
            }
            HStack(spacing: 20) {
                StatItem(value: "\(viewModel.stats.activeWorkflows)", label: "Actifs", icon: "play.circle.fill", color: .n8nGreen)
                StatItem(value: "\(viewModel.stats.totalWorkflows)", label: "Total", icon: "arrow.triangle.branch", color: .n8nBlue)
                StatItem(value: "\(Int(viewModel.stats.successRate))%", label: "Succès", icon: "checkmark.circle.fill", color: .n8nOrange)
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
    
    private var statsGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
            StatCard(title: "Exécutions", value: "\(viewModel.stats.totalExecutions)", icon: "clock.arrow.circlepath", gradient: Color.n8nGradient)
            StatCard(title: "Succès", value: "\(viewModel.stats.successfulExecutions)", icon: "checkmark.circle.fill", gradient: Color.successGradient)
            StatCard(title: "Erreurs", value: "\(viewModel.stats.failedExecutions)", icon: "xmark.circle.fill", gradient: Color.errorGradient)
            StatCard(title: "Temps moyen", value: String(format: "%.1fs", viewModel.stats.averageExecutionTime), icon: "timer", gradient: Color.purpleGradient)
        }
    }
    
    private var recentExecutionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Exécutions récentes").font(.headline)
            if viewModel.recentExecutions.isEmpty {
                EmptyStateView(icon: "clock", message: "Aucune exécution récente")
            } else {
                ForEach(viewModel.recentExecutions.prefix(5)) { execution in
                    ExecutionRow(execution: execution)
                }
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
    
    private var topWorkflowsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Workflows populaires").font(.headline)
            if viewModel.workflows.isEmpty {
                EmptyStateView(icon: "arrow.triangle.branch", message: "Aucun workflow")
            } else {
                ForEach(viewModel.workflows.prefix(5)) { workflow in
                    WorkflowRow(workflow: workflow)
                }
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct StatusBadge: View {
    let status: N8nInstance.InstanceStatus
    var body: some View {
        HStack(spacing: 6) {
            Circle().fill(statusColor).frame(width: 8, height: 8)
            Text(statusText).font(.caption.weight(.medium))
        }
        .padding(.horizontal, 12).padding(.vertical, 6)
        .background(statusColor.opacity(0.15))
        .clipShape(Capsule())
    }
    private var statusColor: Color {
        switch status {
        case .online: return .n8nGreen
        case .offline: return .gray
        case .error: return .n8nRed
        case .unknown: return .n8nYellow
        }
    }
    private var statusText: String {
        switch status {
        case .online: return "En ligne"
        case .offline: return "Hors ligne"
        case .error: return "Erreur"
        case .unknown: return "Inconnu"
        }
    }
}

struct StatItem: View {
    let value: String; let label: String; let icon: String; let color: Color
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon).foregroundColor(color)
            Text(value).font(.title3.bold())
            Text(label).font(.caption2).foregroundColor(.secondary)
        }.frame(maxWidth: .infinity)
    }
}

struct StatCard: View {
    let title: String; let value: String; let icon: String; let gradient: LinearGradient
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: icon).font(.title2).foregroundStyle(.white)
            Spacer()
            Text(value).font(.title.bold()).foregroundColor(.white)
            Text(title).font(.caption).foregroundColor(.white.opacity(0.8))
        }
        .padding()
        .frame(maxWidth: .infinity, minHeight: 120, alignment: .leading)
        .background(gradient)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct EmptyStateView: View {
    let icon: String; let message: String
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon).font(.largeTitle).foregroundColor(.secondary)
            Text(message).font(.subheadline).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
    }
}

struct ExecutionRow: View {
    let execution: Execution
    var body: some View {
        HStack {
            Image(systemName: execution.status.icon)
                .foregroundColor(statusColor)
                .frame(width: 30)
            VStack(alignment: .leading, spacing: 2) {
                Text(execution.workflowName ?? "Workflow \(execution.workflowId)").font(.subheadline.weight(.medium)).lineLimit(1)
                Text(execution.startedAt.timeAgo()).font(.caption2).foregroundColor(.secondary)
            }
            Spacer()
            Text(execution.formattedDuration).font(.caption).foregroundColor(.secondary)
        }
        .padding(.vertical, 8)
    }
    private var statusColor: Color {
        switch execution.status {
        case .success: return .n8nGreen
        case .error, .crashed: return .n8nRed
        case .running: return .n8nBlue
        default: return .secondary
        }
    }
}

struct WorkflowRow: View {
    let workflow: Workflow
    var body: some View {
        HStack {
            Circle().fill(workflow.active ? Color.n8nGreen : Color.gray).frame(width: 8, height: 8)
            Text(workflow.name).font(.subheadline.weight(.medium)).lineLimit(1)
            Spacer()
            Text("\(workflow.nodes.count) nœuds").font(.caption).foregroundColor(.secondary)
        }.padding(.vertical, 8)
    }
}
