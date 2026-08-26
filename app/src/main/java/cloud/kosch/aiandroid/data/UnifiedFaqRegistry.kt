package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.FaqCategory
import cloud.kosch.aiandroid.model.FaqEntry
import java.text.Normalizer
import java.util.Locale

/**
 * Central user-facing FAQ index.
 *
 * The long-lived launcher FAQ stays in [FaqRegistry]. Security & Network is kept in a small,
 * independently reviewable extension while M2.7 is staged, but callers see one combined index.
 */
object UnifiedFaqRegistry {
    private val securityNetworkEntries = listOf(
        faq(
            id = "security-network-n1",
            question = "Was macht Security & Network in N1?",
            answer = "N1 zeigt Androids VPN-Autorisierungsstatus und die Sicherheitsgrenze, startet aber keinen VPN-Tunnel, inspiziert keine Pakete oder DNS-Daten, führt keine Firewall-Regeln aus und ändert weder Routing noch Proxy. Alle Traffic-Zähler bleiben konstruktiv 0.",
            keywords = "security network vpn n1 firewall proxy traffic",
        ),
        faq(
            id = "security-network-consent",
            question = "Startet KoSch ein VPN automatisch?",
            answer = "Nein. KoSch darf Androids VPN-Freigabedialog nur nach einer ausdrücklichen Aktion öffnen. Eine erteilte Autorisierung ist nicht dasselbe wie ein aktives VPN. Ein erkanntes oder unklar vorhandenes anderes VPN muss vor dem Android-Dialog sichtbar bestätigt werden.",
            keywords = "vpn consent zustimmung konflikt autorisierung",
        ),
        faq(
            id = "security-network-privacy",
            question = "Welche Netzwerkdaten speichert N1?",
            answer = "Keine. N1 speichert weder Traffic-Historie noch IPs, Ports, Hostnamen, DNS-Anfragen, Paketdaten, Byte-Zähler, VPN-Zustand oder Proxy-Zugangsdaten. Diese Laufzeitdaten sind auch nicht Bestandteil des portablen Workspace-Backups.",
            keywords = "privacy datenschutz traffic ip port dns backup",
        ),
        faq(
            id = "security-network-play",
            question = "Ist der N1-VpnService schon für Google Play produktionsreif?",
            answer = "Nein. N1 ist eine interne/Beta-Architekturgrenze. Vor einer Play-Produktion mit VpnService muss KoSch einen tatsächlich zulässigen Security-/Firewall-/Network-Anwendungsfall implementieren und validieren, die erforderliche Play-Erklärung und Listing-Offenlegung erfüllen und nötige In-App-Hinweise beziehungsweise Einwilligungen bereitstellen – oder die Komponente aus dem Play-Artefakt ausschließen.",
            keywords = "google play vpnservice declaration release produktionsreif",
        ),
        faq(
            id = "security-network-n2",
            question = "Wann beginnt echte Traffic-Analyse?",
            answer = "Erst in N2 nach separatem Review: reale Weiterleitung ohne Black-Hole, Foreground-Status und Stop-Kontrolle, exakte Permission-Allowlist, begrenzte Metadatenhaltung, UID/App-Zuordnung wo Android sie erlaubt sowie API- und OEM-Evidence. N1 darf diese Fähigkeiten nicht vortäuschen.",
            keywords = "n2 telemetry traffic forwarding uid app evidence",
        ),
    )

    val entries: List<FaqEntry> = FaqRegistry.entries + securityNetworkEntries

    fun search(query: String, category: FaqCategory? = null): List<FaqEntry> {
        val needle = query.searchKey()
        return entries.filter { entry ->
            (category == null || entry.category == category) &&
                (needle.isBlank() || entry.searchText().contains(needle))
        }
    }

    private fun FaqEntry.searchText(): String =
        listOf(question, answer, keywords.joinToString(" "), category.title).joinToString(" ").searchKey()

    private fun String.searchKey(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()

    private fun faq(
        id: String,
        question: String,
        answer: String,
        keywords: String,
    ) = FaqEntry(
        id = id,
        category = FaqCategory.ANDROID,
        question = question,
        answer = answer,
        keywords = keywords.split(' '),
    )
}
