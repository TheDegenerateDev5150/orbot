package org.torproject.android.ui.more

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.torproject.android.OrbotActivity
import org.torproject.android.R
import org.torproject.android.service.util.sendIntentToService
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.OrbotService
import org.torproject.android.ui.AppManagerActivity
import org.torproject.android.ui.OrbotMenuAction
import org.torproject.android.ui.v3onionservice.OnionServiceActivity
import org.torproject.android.ui.v3onionservice.clientauth.ClientAuthActivity

class MoreFragment : Fragment() {
    private var httpPort = -1
    private var socksPort = -1

    private lateinit var tvStatus: TextView

    override fun onAttach(context: Context) {
        super.onAttach(context)

        httpPort = (context as OrbotActivity).portHttp
        socksPort = context.portSocks

        if (view != null) updateStatus()
    }

    private fun updateStatus() {
        val sb = StringBuilder()

        sb.append(getString(R.string.proxy_ports)).append(" ")

        if (httpPort != -1 && socksPort != -1) {
            sb.append("\nHTTP: ").append(httpPort).append(" -  SOCKS: ").append(socksPort)
        } else {
            sb.append(": " + getString(R.string.ports_not_set))
        }

        sb.append("\n\n")

        val manager = requireActivity().packageManager
        val info =
            manager.getPackageInfo(requireActivity().packageName, PackageManager.GET_ACTIVITIES)
        sb.append(getString(R.string.app_name)).append(" ").append(info.versionName).append("\n")
        sb.append("Tor v").append(getTorVersion())

        tvStatus.text = sb.toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_more, container, false)
        tvStatus = view.findViewById(R.id.tvVersion)

        updateStatus()

        val rvMore = view.findViewById<RecyclerView>(R.id.rvMoreActions)
        val ivMascot = view.findViewById<ImageView>(R.id.ivMascot)

        val listItems = listOf(
            OrbotMenuAction(R.string.menu_settings, R.drawable.ic_settings_gear) {
                activity?.startActivityForResult(
                    Intent(context, SettingsActivity::class.java),
                    OrbotActivity.Companion.REQUEST_CODE_SETTINGS
                )
            },
            OrbotMenuAction(R.string.system_vpn_settings, R.drawable.ic_vpn_key) {
                activity?.startActivity(
                    Intent("android.net.vpn.SETTINGS")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            OrbotMenuAction(R.string.btn_choose_apps, R.drawable.ic_choose_apps) {
                activity?.startActivityForResult(
                    Intent(requireActivity(), AppManagerActivity::class.java),
                    OrbotActivity.Companion.REQUEST_VPN_APP_SELECT
                )
            },
            OrbotMenuAction(R.string.menu_log, R.drawable.ic_log) { showLog() },
            OrbotMenuAction(R.string.v3_hosted_services, R.drawable.ic_menu_onion) {
                startActivity(Intent(requireActivity(), OnionServiceActivity::class.java))
            },
            OrbotMenuAction(R.string.v3_client_auth_activity_title, R.drawable.ic_shield) {
                startActivity(Intent(requireActivity(), ClientAuthActivity::class.java))
            },
            OrbotMenuAction(R.string.menu_about, R.drawable.ic_about) {
                AboutDialogFragment().show(
                    requireActivity().supportFragmentManager,
                    AboutDialogFragment.Companion.TAG
                )
            },
            OrbotMenuAction(R.string.menu_exit, R.drawable.ic_exit) { doExit() }
        )
        rvMore.adapter = MoreActionAdapter(listItems)

        val spanCount = if (resources.configuration.screenWidthDp < 600) 2 else 4
        rvMore.layoutManager = GridLayoutManager(requireContext(), spanCount)

        ivMascot.setOnClickListener {
            val scaleX = ObjectAnimator.ofFloat(it, View.SCALE_X, 1f, 1.2f, 1f)
            val scaleY = ObjectAnimator.ofFloat(it, View.SCALE_Y, 1f, 1.2f, 1f)
            val rotate = ObjectAnimator.ofFloat(it, View.ROTATION, 0f, 10f, -10f, 0f)

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, rotate)
                setDuration(500)
                interpolator = BounceInterpolator()
                start()
            }
        }

        return view
    }

    private fun getTorVersion(): String {
        return OrbotService.BINARY_TOR_VERSION.split("-").toTypedArray()[0]
    }

    private fun doExit() {
        val killIntent = Intent(
            requireActivity(), OrbotService::class.java
        ).setAction(OrbotConstants.ACTION_STOP)
            .putExtra(OrbotConstants.ACTION_STOP_FOREGROUND_TASK, true)
        requireContext().sendIntentToService(OrbotConstants.ACTION_STOP_VPN)
        requireContext().sendIntentToService(killIntent)
        requireActivity().finish()
    }

    private fun showLog() {
        (activity as OrbotActivity).showLog()
    }
}