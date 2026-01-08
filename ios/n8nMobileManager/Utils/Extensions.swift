//
//  Extensions.swift
//  n8nMobileManager
//

import SwiftUI

// MARK: - Color Extensions
extension Color {
    static let n8nOrange = Color(red: 255/255, green: 109/255, blue: 0/255)
    static let n8nPink = Color(red: 255/255, green: 64/255, blue: 112/255)
    static let n8nPurple = Color(red: 134/255, green: 64/255, blue: 255/255)
    static let n8nBlue = Color(red: 64/255, green: 158/255, blue: 255/255)
    static let n8nGreen = Color(red: 16/255, green: 185/255, blue: 129/255)
    static let n8nRed = Color(red: 239/255, green: 68/255, blue: 68/255)
    static let n8nYellow = Color(red: 245/255, green: 158/255, blue: 11/255)
    
    static let cardBackground = Color("CardBackground")
    static let primaryText = Color("PrimaryText")
    static let secondaryText = Color("SecondaryText")
    
    // Gradient presets
    static var n8nGradient: LinearGradient {
        LinearGradient(
            colors: [n8nOrange, n8nPink],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    static var successGradient: LinearGradient {
        LinearGradient(
            colors: [n8nGreen, Color(red: 6/255, green: 148/255, blue: 162/255)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    static var errorGradient: LinearGradient {
        LinearGradient(
            colors: [n8nRed, n8nPink],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    static var purpleGradient: LinearGradient {
        LinearGradient(
            colors: [n8nPurple, n8nBlue],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

// MARK: - View Extensions
extension View {
    func glassmorphism() -> some View {
        self
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)
    }
    
    func cardStyle() -> some View {
        self
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
    
    func shimmer(isLoading: Bool) -> some View {
        self
            .redacted(reason: isLoading ? .placeholder : [])
            .shimmering(active: isLoading)
    }
    
    @ViewBuilder
    func `if`<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

// MARK: - Shimmer Effect
struct ShimmerModifier: ViewModifier {
    let active: Bool
    @State private var phase: CGFloat = 0
    
    func body(content: Content) -> some View {
        if active {
            content
                .overlay(
                    GeometryReader { geometry in
                        Color.white
                            .opacity(0.3)
                            .mask(
                                Rectangle()
                                    .fill(
                                        LinearGradient(
                                            gradient: Gradient(colors: [.clear, .white.opacity(0.5), .clear]),
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .offset(x: phase * geometry.size.width)
                            )
                    }
                )
                .onAppear {
                    withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                        phase = 1.5
                    }
                }
        } else {
            content
        }
    }
}

extension View {
    func shimmering(active: Bool = true) -> some View {
        modifier(ShimmerModifier(active: active))
    }
}

// MARK: - Date Extensions
extension Date {
    func timeAgo() -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.localizedString(for: self, relativeTo: Date())
    }
    
    func formatted(style: DateFormatter.Style = .medium) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = style
        formatter.timeStyle = style
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: self)
    }
    
    func formattedTime() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter.string(from: self)
    }
    
    func formattedDate() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM yyyy"
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: self)
    }
    
    func formattedDateTime() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM yyyy 'à' HH:mm"
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: self)
    }
}

// MARK: - String Extensions
extension String {
    func truncated(to length: Int, trailing: String = "...") -> String {
        if self.count > length {
            return String(self.prefix(length)) + trailing
        }
        return self
    }
}

// MARK: - Haptic Feedback
enum HapticFeedback {
    case light
    case medium
    case heavy
    case success
    case warning
    case error
    case selection
    
    func trigger() {
        switch self {
        case .light:
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        case .medium:
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        case .heavy:
            UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
        case .success:
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        case .warning:
            UINotificationFeedbackGenerator().notificationOccurred(.warning)
        case .error:
            UINotificationFeedbackGenerator().notificationOccurred(.error)
        case .selection:
            UISelectionFeedbackGenerator().selectionChanged()
        }
    }
}

// MARK: - Custom Button Styles
struct PrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding()
            .background(
                isEnabled ?
                    AnyView(Color.n8nGradient) :
                    AnyView(Color.gray)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundColor(.n8nOrange)
            .frame(maxWidth: .infinity)
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.n8nOrange, lineWidth: 2)
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct IconButtonStyle: ButtonStyle {
    let color: Color
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(color)
            .padding(8)
            .background(color.opacity(0.15))
            .clipShape(Circle())
            .scaleEffect(configuration.isPressed ? 0.9 : 1)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

// MARK: - Animation Extensions
extension Animation {
    static var smoothSpring: Animation {
        .spring(response: 0.4, dampingFraction: 0.75)
    }
    
    static var gentleBounce: Animation {
        .spring(response: 0.5, dampingFraction: 0.6, blendDuration: 0.5)
    }
}
