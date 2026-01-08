//
//  WorkflowsView.swift
//  n8nMobileManager
//

import SwiftUI

struct WorkflowsView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = WorkflowsViewModel()
    @State private var searchText = ""
    @State private var showActiveOnly = false
    @State private var selectedWorkflow: Workflow?
    
    var filteredWorkflows: [Workflow] {
        var result = viewModel.workflows
        if showActiveOnly { result = result.filter { $0.active } }
        if !searchText.isEmpty { result = result.filter { $0.name.localizedCaseInsensitiveContains(searchText) } }
        return result
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                if viewModel.isLoading && viewModel.workflows.isEmpty {
                    ProgressView("Chargement...").frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.workflows.isEmpty {
                    emptyState
                } else {
                    workflowList
                }
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle("Workflows")
            .searchable(text: $searchText, prompt: "Rechercher...")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Toggle("Actifs uniquement", isOn: $showActiveOnly)
                        Button(action: { Task { await viewModel.refresh() } }) {
                            Label("Actualiser", systemImage: "arrow.clockwise")
                        }
                    } label: { Image(systemName: "line.3.horizontal.decrease.circle") }
                }
            }
            .refreshable { await viewModel.refresh() }
            .task { if let instance = appState.currentInstance { await viewModel.load(instance: instance) } }
            .sheet(item: $selectedWorkflow) { workflow in
                WorkflowDetailView(workflow: workflow, viewModel: viewModel)
            }
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "arrow.triangle.branch").font(.system(size: 60)).foregroundColor(.secondary)
            Text("Aucun workflow").font(.title2.bold())
            Text("Créez des workflows dans n8n pour les voir ici").font(.subheadline).foregroundColor(.secondary)
        }
    }
    
    private var workflowList: some View {
        List {
            ForEach(filteredWorkflows) { workflow in
                WorkflowListItem(workflow: workflow, onToggle: { toggleWorkflow(workflow) }, onRun: { runWorkflow(workflow) })
                    .contentShape(Rectangle())
                    .onTapGesture { selectedWorkflow = workflow }
            }
        }
        .listStyle(.insetGrouped)
    }
    
    private func toggleWorkflow(_ workflow: Workflow) {
        HapticFeedback.medium.trigger()
        Task {
            if workflow.active {
                await viewModel.deactivateWorkflow(id: workflow.id)
            } else {
                await viewModel.activateWorkflow(id: workflow.id)
            }
        }
    }
    
    private func runWorkflow(_ workflow: Workflow) {
        HapticFeedback.medium.trigger()
        Task { await viewModel.executeWorkflow(id: workflow.id) }
    }
}

struct WorkflowListItem: View {
    let workflow: Workflow
    let onToggle: () -> Void
    let onRun: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            Circle().fill(workflow.active ? Color.n8nGreen : Color.gray.opacity(0.5)).frame(width: 12, height: 12)
            VStack(alignment: .leading, spacing: 4) {
                Text(workflow.name).font(.headline).lineLimit(1)
                HStack(spacing: 8) {
                    Label("\(workflow.nodes.count)", systemImage: "circle.grid.2x2").font(.caption2)
                    if !workflow.tags.isEmpty {
                        Text(workflow.tags.first?.name ?? "").font(.caption2).padding(.horizontal, 6).padding(.vertical, 2)
                            .background(Color.n8nOrange.opacity(0.2)).clipShape(Capsule())
                    }
                }.foregroundColor(.secondary)
            }
            Spacer()
            HStack(spacing: 8) {
                Button(action: onRun) {
                    Image(systemName: "play.fill").foregroundColor(.n8nGreen)
                }.buttonStyle(.plain)
                Toggle("", isOn: .constant(workflow.active)).labelsHidden().tint(.n8nOrange)
                    .onTapGesture { onToggle() }
            }
        }.padding(.vertical, 4)
    }
}

struct WorkflowDetailView: View {
    let workflow: Workflow
    @ObservedObject var viewModel: WorkflowsViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var isRunning = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    headerCard
                    nodesSection
                    actionsSection
                }.padding()
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle(workflow.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Fermer") { dismiss() } }
            }
        }
    }
    
    private var headerCard: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Circle().fill(workflow.active ? Color.n8nGreen : Color.gray).frame(width: 10, height: 10)
                        Text(workflow.active ? "Actif" : "Inactif").font(.subheadline.weight(.medium))
                    }
                    Text("Modifié \(workflow.updatedAt.timeAgo())").font(.caption).foregroundColor(.secondary)
                }
                Spacer()
                Text("\(workflow.nodes.count) nœuds").font(.caption).padding(.horizontal, 12).padding(.vertical, 6)
                    .background(Color.n8nBlue.opacity(0.2)).clipShape(Capsule())
            }
            if !workflow.tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(workflow.tags) { tag in
                            Text(tag.name).font(.caption).padding(.horizontal, 10).padding(.vertical, 4)
                                .background(Color.n8nOrange.opacity(0.2)).clipShape(Capsule())
                        }
                    }
                }
            }
        }.padding().background(Color(UIColor.secondarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 16))
    }
    
    private var nodesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Nœuds").font(.headline)
            ForEach(workflow.nodes) { node in
                HStack(spacing: 12) {
                    Image(systemName: node.icon).foregroundColor(.n8nOrange).frame(width: 30)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(node.name).font(.subheadline.weight(.medium))
                        Text(node.displayType).font(.caption).foregroundColor(.secondary)
                    }
                    Spacer()
                    if node.disabled == true {
                        Text("Désactivé").font(.caption2).foregroundColor(.secondary)
                    }
                }.padding(.vertical, 6)
            }
        }.padding().background(Color(UIColor.secondarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 16))
    }
    
    private var actionsSection: some View {
        VStack(spacing: 12) {
            Button(action: { runWorkflow() }) {
                HStack {
                    if isRunning { ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white)) }
                    else { Image(systemName: "play.fill") }
                    Text("Exécuter maintenant")
                }
            }.buttonStyle(PrimaryButtonStyle()).disabled(isRunning)
            
            Button(action: { toggleWorkflow() }) {
                HStack {
                    Image(systemName: workflow.active ? "pause.fill" : "play.fill")
                    Text(workflow.active ? "Désactiver" : "Activer")
                }
            }.buttonStyle(SecondaryButtonStyle())
        }
    }
    
    private func runWorkflow() {
        isRunning = true
        HapticFeedback.medium.trigger()
        Task {
            await viewModel.executeWorkflow(id: workflow.id)
            isRunning = false
            HapticFeedback.success.trigger()
        }
    }
    
    private func toggleWorkflow() {
        HapticFeedback.medium.trigger()
        Task {
            if workflow.active { await viewModel.deactivateWorkflow(id: workflow.id) }
            else { await viewModel.activateWorkflow(id: workflow.id) }
            dismiss()
        }
    }
}
