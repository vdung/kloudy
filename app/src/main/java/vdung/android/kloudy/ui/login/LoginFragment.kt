package vdung.android.kloudy.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.databinding.LoginFragmentBinding
import javax.inject.Inject

class LoginFragment : DaggerFragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: LoginFragmentBinding
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = LoginFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(LoginViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.loginResult.observe(this, Observer {
            when (it) {
                is Result.Error -> {
                    Toast.makeText(requireContext(), it.error.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}
