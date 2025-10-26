package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class OAuthInfoDialog extends Dialog
{
    private final String authorizationUrl;
    private final String callbackUrl;

    public OAuthInfoDialog(Shell parentShell, String authorizationUrl, String callbackUrl)
    {
        super(parentShell);
        this.authorizationUrl = authorizationUrl;
        this.callbackUrl = callbackUrl;
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText("OAuth Login Information");
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayoutFactory.fillDefaults().numColumns(1).extendedMargins(10, 10, 10, 10).applyTo(container);

        // Title
        var titleLabel = new Label(container, SWT.CENTER);
        titleLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING1);
        GridDataFactory.swtDefaults().hint(600, SWT.DEFAULT).applyTo(titleLabel);
        titleLabel.setText("OAuth Login URLs");

        // Description
        var descriptionLabel = new Label(container, SWT.WRAP);
        GridDataFactory.swtDefaults().hint(600, SWT.DEFAULT).applyTo(descriptionLabel);
        descriptionLabel.setText("The following URLs will be used for the OAuth authentication process:");

        // Authorization URL section
        var authLabel = new Label(container, SWT.NONE);
        GridDataFactory.swtDefaults().applyTo(authLabel);
        authLabel.setText("Authorization URL (click to copy):");

        var authText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        GridDataFactory.swtDefaults().hint(600, 80).applyTo(authText);
        authText.setText(authorizationUrl);
        authText.addMouseListener(new org.eclipse.swt.events.MouseAdapter()
        {
            @Override
            public void mouseDown(org.eclipse.swt.events.MouseEvent e)
            {
                authText.selectAll();
                authText.copy();
                // Clear selection after copying
                authText.setSelection(0, 0);
            }
        });

        // Callback URL section
        var callbackLabel = new Label(container, SWT.NONE);
        GridDataFactory.swtDefaults().applyTo(callbackLabel);
        callbackLabel.setText("Callback URL (click to copy):");

        var callbackText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        GridDataFactory.swtDefaults().hint(600, 60).applyTo(callbackText);
        callbackText.setText(callbackUrl);
        callbackText.addMouseListener(new org.eclipse.swt.events.MouseAdapter()
        {
            @Override
            public void mouseDown(org.eclipse.swt.events.MouseEvent e)
            {
                callbackText.selectAll();
                callbackText.copy();
                // Clear selection after copying
                callbackText.setSelection(0, 0);
            }
        });

        // Instructions
        var instructionsLabel = new Label(container, SWT.WRAP);
        GridDataFactory.swtDefaults().hint(600, SWT.DEFAULT).applyTo(instructionsLabel);
        instructionsLabel.setText("Click on any URL to copy it to clipboard. Close this dialog to continue with the authentication.");

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, "Continue with Login", true);
        createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
    }
}